#!/usr/bin/env node

/**
 * MCP Auto Re-Login Stdio Bridge
 * Bridges AI Agent stdio to a remote MCP server with OIDC auth,
 * OS keychain token caching, serialized sends, and hardened auth failure handling.
 */

const http = require('http');
const axios = require('axios');
const crypto = require('crypto');
const { EventSource } = require('eventsource');
const readline = require('readline');
const { execSync, spawnSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');

// ==========================================
// 1. CONFIGURATION
// ==========================================
const { parseArgs } = require('util');

const { values: flags, positionals } = parseArgs({
    args: process.argv.slice(2),
    options: {
        'issuer-url': { type: 'string' },
        'client-id':  { type: 'string' },
        'scope':      { type: 'string' },
        'port':       { type: 'string' },
    },
    allowPositionals: true,
});

if (positionals.length < 1) {
    console.error('Usage: mcp-proxy <target-url> [--issuer-url <url>] [--client-id <id>] [--scope <scope>] [--port <port>]');
    process.exit(1);
}

const TARGET_URL  = positionals[0];
const ISSUER_URL  = flags['issuer-url'] ?? 'your-issuer-url';
const CLIENT_ID   = flags['client-id']  ?? 'static.client.id';
const OIDC_SCOPE  = flags['scope']      ?? 'openid email profile profile_preferred_username';
const LOCAL_PORT  = flags['port']       ? parseInt(String(flags['port']), 10) : 9080;
const REDIRECT_URI = `http://localhost:${LOCAL_PORT}/callback`;

const KEYCHAIN_SERVICE = 'mcp-proxy';
const KEYCHAIN_ACCOUNT = 'token';
const TOKEN_FILE = path.join(os.homedir(), '.config', 'mcp-proxy', 'token'); // Linux fallback
const MAX_AUTH_RETRIES = 3;
const AUTH_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
const JSONRPC_AUTH_ERROR = -32001;

// ==========================================
// 2. STATE MACHINE
// ==========================================
const State = Object.freeze({
    IDLE: 'IDLE',
    AUTHENTICATING: 'AUTHENTICATING',
    CONNECTED: 'CONNECTED',
    ERROR: 'ERROR',
});

let state = State.IDLE;
let authRetryCount = 0;
let authTimeoutHandle = null;

let authEndpoint = null;
let tokenEndpoint = null;
let accessToken = null;
// Per-flow object — replaced on each new attempt to prevent state/codeVerifier races
let currentAuthFlow = null; // { codeVerifier, state }

let pendingMessages = [];
let esClient = null;
let postEndpoint = null;
let mcpSessionId = null;
let callbackServer = null;

// Serial send chain — ensures messages are dispatched one at a time
let sendChain = Promise.resolve();

function setState(newState) {
    if (state !== newState) {
        console.error(`State: ${state} -> ${newState}`);
        state = newState;
    }
}

function isUnauthorized(err) {
    return err?.response?.status === 401 || err?.status === 401;
}

// ==========================================
// 3. TOKEN KEYCHAIN HELPERS
// ==========================================

// Platform-specific keychain operations — selected once at startup to avoid
// repeating the darwin/win32/linux dispatch in every function.
const keychain = (() => {
    if (process.platform === 'darwin') {
        return {
            load() {
                const result = spawnSync(
                    'security',
                    ['find-generic-password', '-s', KEYCHAIN_SERVICE, '-a', KEYCHAIN_ACCOUNT, '-w'],
                    { stdio: ['ignore', 'pipe', 'ignore'], encoding: 'utf8' }
                );
                if (result.status !== 0) {
                    return null;
                }
                return (result.stdout || '').trim();
            },
            save(token) {
                spawnSync(
                    'security',
                    ['add-generic-password', '-U', '-s', KEYCHAIN_SERVICE, '-a', KEYCHAIN_ACCOUNT, '-w', token]
                );
            },
            delete() {
                spawnSync(
                    'security',
                    ['delete-generic-password', '-s', KEYCHAIN_SERVICE, '-a', KEYCHAIN_ACCOUNT],
                    { stdio: 'ignore' }
                );
            },
        };
    } else if (process.platform === 'win32') {
        function psExec(script) {
            const cmd = Buffer.from(script, 'utf16le').toString('base64');
            return execSync(`powershell -NoProfile -NonInteractive -EncodedCommand ${cmd}`,
                { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
        }
        return {
            load() {
                return psExec(`[System.Text.Encoding]::UTF8.GetString([System.Security.Cryptography.ProtectedData]::Unprotect([System.IO.File]::ReadAllBytes("$env:APPDATA\\${KEYCHAIN_SERVICE}-${KEYCHAIN_ACCOUNT}.bin"),$null,'CurrentUser'))`);
            },
            save(token) {
                const escaped = token.replace(/'/g, "''");
                psExec(`Add-Type -AssemblyName System.Security; $b=[System.Text.Encoding]::UTF8.GetBytes('${escaped}'); [System.IO.File]::WriteAllBytes("$env:APPDATA\\${KEYCHAIN_SERVICE}-${KEYCHAIN_ACCOUNT}.bin",[System.Security.Cryptography.ProtectedData]::Protect($b,$null,'CurrentUser'))`);
            },
            delete() {
                try { psExec(`Remove-Item "$env:APPDATA\\${KEYCHAIN_SERVICE}-${KEYCHAIN_ACCOUNT}.bin" -ErrorAction SilentlyContinue`); } catch { }
            },
        };
    } else {
        return {
            load() {
                try {
                    return execSync(
                        `secret-tool lookup service ${KEYCHAIN_SERVICE} account ${KEYCHAIN_ACCOUNT}`,
                        { stdio: ['ignore', 'pipe', 'ignore'] }
                    ).toString().trim();
                } catch {
                    return fs.readFileSync(TOKEN_FILE, 'utf8').trim();
                }
            },
            save(token) {
                // Use spawnSync with an args array to avoid shell injection via the token value
                const result = spawnSync('secret-tool', [
                    'store', '--label=MCP Proxy Token',
                    'service', KEYCHAIN_SERVICE,
                    'account', KEYCHAIN_ACCOUNT,
                ], { input: token, encoding: 'utf8' });
                if (result.error || result.status !== 0) {
                    fs.mkdirSync(path.dirname(TOKEN_FILE), { recursive: true });
                    fs.writeFileSync(TOKEN_FILE, token, { mode: 0o600 });
                }
            },
            delete() {
                try { execSync(`secret-tool clear service ${KEYCHAIN_SERVICE} account ${KEYCHAIN_ACCOUNT}`, { stdio: 'ignore' }); } catch { }
                try { fs.unlinkSync(TOKEN_FILE); } catch { }
            },
        };
    }
})();

function isTokenExpired(token) {
    try {
        const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());
        return !payload.exp || payload.exp < Math.floor(Date.now() / 1000);
    } catch {
        return true; // treat unparseable token as expired
    }
}

function loadCachedToken() {
    try {
        const token = keychain.load();
        if (!token) return null;
        if (isTokenExpired(token)) {
            console.error('Cached token is expired, discarding.');
            keychain.delete();
            return null;
        }
        return token;
    } catch {
        return null;
    }
}

function saveToken(token) {
    keychain.save(token);
}

// ==========================================
// 4. AUTHENTICATION LOGIC
// ==========================================
function drainQueueWithError(message) {
    while (pendingMessages.length > 0) {
        const msg = pendingMessages.shift();
        try {
            const parsed = JSON.parse(msg);
            if (parsed.id != null) {
                process.stdout.write(
                    JSON.stringify({ jsonrpc: '2.0', id: parsed.id, error: { code: JSONRPC_AUTH_ERROR, message } }) + '\n'
                );
            }
        } catch { }
    }
}

function cancelAuthTimeout() {
    if (authTimeoutHandle) {
        clearTimeout(authTimeoutHandle);
        authTimeoutHandle = null;
    }
}

function ensureCallbackServer() {
    if (callbackServer) return Promise.resolve();
    return new Promise(resolve => {
        callbackServer = http.createServer(handleCallback);
        callbackServer.listen(LOCAL_PORT, '127.0.0.1', () => {
            console.error(`Callback server on http://127.0.0.1:${LOCAL_PORT}`);
            resolve();
        });
    });
}

async function triggerAuthentication() {
    if (!authEndpoint) return; // OIDC discovery not complete yet; initialize() will call us when ready
    if (state === State.AUTHENTICATING || state === State.ERROR) return;

    authRetryCount++;
    if (authRetryCount > MAX_AUTH_RETRIES) {
        console.error(`Auth failed after ${MAX_AUTH_RETRIES} retries.`);
        setState(State.ERROR);
        drainQueueWithError('Authentication failed: max retries exceeded');
        return;
    }

    setState(State.AUTHENTICATING);
    console.error(`Opening browser for auth (attempt ${authRetryCount}/${MAX_AUTH_RETRIES})...`);
    await ensureCallbackServer();

    // New per-flow object — replaces any previous flow so concurrent callbacks can't cross-contaminate
    currentAuthFlow = {
        codeVerifier: crypto.randomBytes(32).toString('base64url'),
        state: crypto.randomBytes(16).toString('base64url'),
    };
    const codeChallenge = crypto.createHash('sha256').update(currentAuthFlow.codeVerifier).digest('base64url');
    const loginUrl = `${authEndpoint}?` + new URLSearchParams({
        client_id: CLIENT_ID,
        response_type: 'code',
        redirect_uri: REDIRECT_URI,
        scope: OIDC_SCOPE,
        code_challenge: codeChallenge,
        code_challenge_method: 'S256',
        state: currentAuthFlow.state,
    });

    authTimeoutHandle = setTimeout(() => {
        console.error(`Auth timed out after ${AUTH_TIMEOUT_MS / 1000}s.`);
        authRetryCount = 0; // reset so future attempts get a full set of retries
        setState(State.IDLE);
    }, AUTH_TIMEOUT_MS);

    console.error(`\nOpen this URL in your browser to authenticate:\n\n  ${loginUrl}\n`);
    try {
        const { default: open } = await import('open');
        await open(loginUrl);
    } catch (e) {
        console.error('Could not auto-open browser. Please open the URL above manually.');
    }
}

async function handleCallback(req, res) {
    const url = new URL(req.url, `http://127.0.0.1:${LOCAL_PORT}`);
    if (req.method !== 'GET' || url.pathname !== '/callback') {
        res.writeHead(404).end();
        return;
    }

    const authCode = url.searchParams.get('code');
    const returnedState = url.searchParams.get('state');

    // Validate authorization code: non-empty, safe characters only, reasonable length
    if (!authCode || !/^[\w.\-~]+$/.test(authCode) || authCode.length > 512) {
        res.writeHead(400, { 'Content-Type': 'text/plain' }).end('Invalid or missing authorization code.');
        return;
    }
    if (!returnedState || !currentAuthFlow || returnedState !== currentAuthFlow.state) {
        console.error('OAuth state mismatch — possible CSRF. Rejecting callback.');
        res.writeHead(400, { 'Content-Type': 'text/plain' }).end('State mismatch. Possible CSRF attack detected.');
        return;
    }

    cancelAuthTimeout();

    // Consume the flow — clear before async work to prevent reuse on a second callback
    const { codeVerifier } = currentAuthFlow;
    currentAuthFlow = null;

    try {
        const response = await axios.post(tokenEndpoint, new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: CLIENT_ID,
            redirect_uri: REDIRECT_URI,
            code: authCode,
            code_verifier: codeVerifier,
        }), {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        });

        accessToken = response.data.access_token;
        saveToken(accessToken);
        authRetryCount = 0;
        res.writeHead(200, { 'Content-Type': 'text/html' }).end(`<!DOCTYPE html><html lang="en"><body>
            <p>Authentication successful! This window will close automatically.</p>
            <script>window.close();</script>
        </body></html>`);
        console.error('Access token acquired and saved to Keychain.');

    } catch (error) {
        const detail = error.response ? JSON.stringify(error.response.data) : error.message;
        console.error('Error exchanging token:', detail);
        res.writeHead(500, { 'Content-Type': 'text/html' }).end(`<!DOCTYPE html><html lang="en"><body>
            <p>Authentication failed. Check terminal logs.</p>
        </body></html>`);
        setState(State.IDLE); // allow retry on next incoming message
        return;
    }

    // Callback server is no longer needed — close it to free the port and reduce attack surface
    if (callbackServer) {
        callbackServer.close();
        callbackServer = null;
    }

    startBridge();
}

// ==========================================
// 5. BRIDGE LOGIC
// ==========================================
function closeSseStream() {
    if (esClient) {
        esClient.close();
        esClient = null;
    }
}

function startBridge() {
    closeSseStream(); // discard any stale connection from a previous session
    setState(State.CONNECTED);
    console.error('Ready. Waiting for MCP session via first initialize request...');
    mcpSessionId = null;
    postEndpoint = TARGET_URL;
    flushMessageQueue();
}

function openSseStream() {
    closeSseStream();

    console.error(`Opening SSE stream with session ${mcpSessionId}...`);
    esClient = new EventSource(TARGET_URL, {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Mcp-Session-Id': mcpSessionId,
        },
    });

    esClient.addEventListener('message', (event) => {
        process.stdout.write(event.data + '\n');
    });

    esClient.onerror = (err) => {
        if (isUnauthorized(err)) {
            console.error('401 on SSE stream. Triggering re-auth...');
            closeSseStream();
            setState(State.IDLE);
            triggerAuthentication();
        } else {
            console.error('SSE stream error:', err?.message ?? 'unknown');
        }
    };
}

function enqueueForSend(msg) {
    sendChain = sendChain.then(async () => {
        // Re-check state at execution time; re-queue if no longer connected
        if (state !== State.CONNECTED || !postEndpoint || !accessToken) {
            pendingMessages.push(msg);
            return;
        }
        await sendToRemote(msg);
    }).catch(err => {
        console.error('Unexpected send chain error:', err.message);
    });
}

function flushMessageQueue() {
    const toSend = pendingMessages.splice(0);
    for (const msg of toSend) {
        enqueueForSend(msg);
    }
}

async function sendToRemote(messagePayload) {
    const headers = {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream, application/json',
    };
    if (mcpSessionId) headers['Mcp-Session-Id'] = mcpSessionId;

    try {
        const response = await axios.post(postEndpoint, messagePayload, { headers });

        if (!mcpSessionId && response.headers['mcp-session-id']) {
            mcpSessionId = response.headers['mcp-session-id'];
            console.error(`MCP Session established: ${mcpSessionId}`);
            openSseStream();
        }

        if (response.data) {
            const raw = typeof response.data === 'string' ? response.data : JSON.stringify(response.data);
            if (raw.startsWith('data:') || raw.includes('\ndata:')) {
                for (const line of raw.split('\n')) {
                    if (line.startsWith('data:')) {
                        process.stdout.write(line.slice(5).replace(/^ /, '') + '\n');
                    }
                }
            } else {
                process.stdout.write(raw + '\n');
            }
        }
    } catch (error) {
        if (isUnauthorized(error)) {
            console.error('401 on POST. Re-queuing message and triggering re-auth...');
            closeSseStream(); // close stale SSE before starting re-auth
            pendingMessages.unshift(messagePayload);
            setState(State.IDLE);
            triggerAuthentication();
        } else {
            const status = error.response?.status ?? 'N/A';
            const body = error.response?.data ? JSON.stringify(error.response.data) : error.message;
            console.error(`POST failed [HTTP ${status}]: ${body}`);
        }
    }
}

// ==========================================
// 6. STDIN LISTENER & STARTUP
// ==========================================
const rl = readline.createInterface({ input: process.stdin, terminal: false });

rl.on('line', (line) => {
    if (state === State.CONNECTED && postEndpoint && accessToken) {
        enqueueForSend(line);
    } else {
        pendingMessages.push(line);
        if (state === State.IDLE) triggerAuthentication();
    }
});

async function initialize() {
    try {
        console.error(`Fetching OIDC config from ${ISSUER_URL}...`);
        const baseUrl = ISSUER_URL.replace(/\/$/, '');
        const { data } = await axios.get(`${baseUrl}/.well-known/openid-configuration`);
        authEndpoint = data.authorization_endpoint;
        tokenEndpoint = data.token_endpoint;

        accessToken = loadCachedToken();
        if (accessToken) {
            console.error('Cached token found. Skipping browser login.');
            startBridge();
        } else {
            triggerAuthentication();
        }
    } catch (error) {
        console.error('\nFailed to initialize OIDC Discovery:', error.message);
        process.exit(1);
    }
}

initialize();
