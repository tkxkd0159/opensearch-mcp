# OpenSearch MCP User Guide

## Overview

This MCP server exposes OpenSearch cluster health, state, shards, segments, nodes, allocation, and a generic API fallback through MCP tools.

Use HTTP when you want a long-running MCP endpoint, remote-client style configuration, or ad-hoc `clusterUrl` access.
Use `stdio` when you want the client to launch the server process directly.

## Run the Server Locally

The local repo config registers a `local` cluster that points to `https://localhost:9200` with demo credentials from `application-local.yml`.

### HTTP transport

Run the HTTP MCP server with the local profile:

```sh
./gradlew :opensearch-mcp-http:runLocalJar
```

- MCP endpoint: `http://localhost:8080/mcp`
- Actuator: `http://localhost:8081/actuator`

If you want a local OpenSearch cluster and HTTP MCP server together:

```sh
docker compose up --build
```

### `stdio` transport

Run the stdio MCP server with the local profile:

```sh
./gradlew :opensearch-mcp-stdio:runLocalJar
```

If you want to use the packaged `stdio` jar in a client config, build it first:

```sh
./gradlew :opensearch-mcp-stdio:bootJar
```

<details>
    <summary><strong>Connect from Claude</strong></summary>

The examples below come from [`.mcp.example.json`](.mcp.example.json). They use repo-relative paths. If your Claude client starts outside this repository, replace relative paths with absolute paths.

### Registered cluster over HTTP

Use this when the HTTP MCP server is already running and has registered clusters such as `local`:

```json
{
  "mcpServers": {
    "o8h-mcp-server-http": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### Registered cluster over `stdio`

Use this when you want Claude to launch the `stdio` server directly:

```json
{
  "mcpServers": {
    "o8h-mcp-server-stdio": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "./build/libs/opensearch-mcp-stdio-latest-SNAPSHOT.jar",
        "--spring.config.additional-location=file:./opensearch-mcp-stdio/src/main/resources/application-local.yml"
      ]
    }
  }
}
```

Build the jar before using this config:

```sh
./gradlew :opensearch-mcp-stdio:bootJar
```

### Ad-hoc HTTP access with request headers

Use this when you do not want to pre-register a cluster in server config and instead want tool calls to pass `clusterUrl` directly:

```json
{
  "mcpServers": {
    "o8h-mcp-server-http-adhoc": {
      "type": "http",
      "url": "http://localhost:8080/mcp",
      "headers": {
        "X-OpenSearch-Authorization": "Basic YWRtaW46TG9jYWxBZG1pbjEyMyE=",
        "X-OpenSearch-SSL-Disabled": "true"
      }
    }
  }
}
```

The example authorization header is for the local demo cluster (`admin:LocalAdmin123!`). Replace it for any non-local environment.

### Advanced: OIDC proxy bridge

The example config also includes an `auth-proxy-o8g` command entry. It expects `./proxy/mcp-proxy-1.0.0.tgz`, which is not committed in this repository. Build it first:

```sh
cd proxy
npm pack
```

Then use the command entry from the example config, replacing the placeholder issuer and client values:

```json
{
  "mcpServers": {
    "auth-proxy-o8g": {
      "command": "npx",
      "args": [
        "-y",
        "file:./proxy/mcp-proxy-1.0.0.tgz",
        "http://localhost:8080/mcp",
        "--issuer-url",
        "your-issuer-url",
        "--port",
        "9080",
        "--client-id",
        "your-client-id",
        "--scope",
        "openid email profile profile_preferred_username"
      ]
    }
  }
}
```

This bridge wraps the HTTP MCP server and opens a local OIDC callback on `127.0.0.1:9080`.
</details>

<details>
    <summary><strong>Connect from Codex</strong></summary>

The examples below come from [`.codex/config.example.toml`](.codex/config.example.toml). As with the Claude examples, convert relative paths to absolute ones if Codex does not start in this repository root.

### Registered cluster over HTTP

```toml
[mcp_servers.o8h_mcp_server_http]
url = "http://localhost:8080/mcp"
```

### Registered cluster over `stdio`

```toml
[mcp_servers.o8h_mcp_server_stdio]
command = "java"
args = [
    "-jar", "./build/libs/opensearch-mcp-stdio-latest-SNAPSHOT.jar",
    "--spring.config.additional-location=file:./opensearch-mcp-stdio/src/main/resources/application-local.yml",
]
```

### Ad-hoc HTTP access with request headers

```toml
[mcp_servers.o8h_mcp_server_http_adhoc]
url = "http://localhost:8080/mcp"
http_headers = { X-OpenSearch-Authorization = "Basic YWRtaW46TG9jYWxBZG1pbjEyMyE=", X-OpenSearch-SSL-Disabled = "true" }
```

### Advanced: OIDC proxy bridge

Build the tarball first with `cd proxy && npm pack`, then use the example entry:

```toml
[mcp_servers.auth_proxy_o8g]
command = "npx"
args = [
    "-y",
    "file:./proxy/mcp-proxy-1.0.0.tgz",
    "http://localhost:8080/mcp",
    "--issuer-url",
    "your-issuer-url",
    "--port",
    "9080",
    "--client-id",
    "your-client-id",
    "--scope",
    "openid email profile profile_preferred_username",
]
```
</details>


## How to Use This MCP Server

For the full tool list and parameter reference, see [README.md](README.md).

### Choose the right connection mode

- Use `clusterName` when the target cluster is registered in server config. Start with `listClusters` to discover available names.
- Use `clusterUrl` only on the HTTP transport when you want one-off access to a cluster that is not registered.
- If you use `clusterUrl`, configure `X-OpenSearch-Authorization` on the MCP request path. The `stdio` transport does not support ad-hoc `clusterUrl`.

### Typical workflow

1. Discover registered clusters.
2. Check health or state.
3. Drill into shards, nodes, hot threads, segments, or allocation.
4. Use `callApi` only when a dedicated tool does not cover the endpoint you need.

### Example prompts

Registered cluster:

```text
List the registered OpenSearch clusters and show me which one looks local.
```

```text
Check cluster health for the `local` cluster and summarize any unassigned shards.
```

```text
Show shard allocation for the `books` index on the `local` cluster.
```

```text
Get node information for the `local` cluster, limited to `jvm,thread_pool` metrics.
```

```text
Show hot threads for the `local` cluster and highlight anything that looks CPU-heavy.
```

Ad-hoc HTTP cluster:

```text
Use clusterUrl `https://localhost:9200` to get cluster health and explain whether the cluster is healthy.
```

Generic API fallback:

```text
Call `/_search` on the `local` cluster and return the latest five log documents from the last two hours.
```

Write example with `callApi`:

```text
Add a document to the `logs` index on the `local` cluster with message `MCP test` and the current UTC timestamp.

Return documents whose timestamp is within the last 120 minutes from the `logs` index on the `local` OpenSearch cluster.
```

Use dedicated tools first for common diagnostics. Use `callApi` for unsupported endpoints, custom query parameters, or raw request bodies.
