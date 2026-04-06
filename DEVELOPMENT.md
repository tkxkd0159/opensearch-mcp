# OpenSearch MCP Development Guide

## Overview

This repository contains three Gradle modules:

- `opensearch-mcp-core`: shared OpenSearch client configuration, cluster resolution, and MCP tool implementations
- `opensearch-mcp-http`: Spring Boot HTTP transport on port `8080` with actuator on `8081`
- `opensearch-mcp-stdio`: Spring Boot `stdio` transport

Shared Gradle conventions live in `build-logic/`. The `proxy/` directory contains the Node-based OIDC bridge example. Local cluster and smoke-test helpers live in `compose.yml`, `docker/`, and `scripts/`.

## Prerequisites

- JDK 25
- Docker, if you want to run the local OpenSearch stack or Testcontainers-backed integration tests
- Use the Gradle wrapper from the repository root

## Build and Test

### Core verification commands

```sh
./gradlew build
./gradlew check
./gradlew test
./gradlew spotlessApply
./gradlew jacocoAggregateReport
./gradlew javadoc
```

### Module-focused commands

```sh
./gradlew :opensearch-mcp-core:test
./gradlew :opensearch-mcp-core:integrationTest
./gradlew :opensearch-mcp-http:test
./gradlew :opensearch-mcp-stdio:test
```

`check` includes Spotless, aggregate coverage verification, Javadoc generation, and the shared core integration test suite.

## Run Locally

### Build boot jars

```sh
./gradlew :opensearch-mcp-http:bootJar :opensearch-mcp-stdio:bootJar
```

Both boot jars are written to `build/libs/` at the repository root.

### Run the transports

```sh
./gradlew :opensearch-mcp-http:runLocalJar
./gradlew :opensearch-mcp-stdio:runLocalJar
```

- HTTP MCP endpoint: `http://localhost:8080/mcp`
- Actuator endpoint: `http://localhost:8081/actuator`

The local profile registers a `local` cluster from each module's `application-local.yml`.

### Run the local stack with Docker Compose

```sh
docker compose up --build
```

This starts:

- a two-node OpenSearch cluster
- the HTTP MCP server configured against that cluster

For a quick smoke test after the server is up:

```sh
./scripts/test-cluster-health.sh
./scripts/test-general-api.sh
```

Automated integration coverage for the shared tool layer lives in `opensearch-mcp-core` and runs via Testcontainers.

## Contributor Workflow

- Start feature or bugfix work in a fresh git worktree instead of editing directly in the base checkout.
- Preserve the existing module structure and public tool behavior unless the change explicitly requires a contract change.
- Add or update tests with behavior changes, especially in `opensearch-mcp-core/src/test/java`.
- Keep local-only overrides out of git. The repo already ignores `_test.yml`, `build/`, and `.worktrees/`.
- Before opening a pull request, use [`.github/pull_request_template.md`](.github/pull_request_template.md) as the verification checklist.

## Documentation and Client Examples

- End-user setup and client configuration live in [USER_GUIDE.md](USER_GUIDE.md).
- The root [README.md](README.md) is the public landing page and tool reference.
- Claude and Codex MCP client examples live in [`.mcp.example.json`](.mcp.example.json) and [`.codex/config.example.toml`](.codex/config.example.toml).


## MCP Flow (Streamable HTTP transport)

| Endpoint | Method | Purpose                                            |
| -------- | ------ | -------------------------------------------------- |
| /mcp     | POST   | Send MCP requests (initialize, tool calls, etc.)   |
| /mcp     | GET    | Open a stream to receive server-initiated messages |
| /mcp     | DELETE | Terminate the session                              |

1. Handshake (POST): The client sends an initialize request. The server creates a session and returns a unique Mcp-Session-Id.
2. Streaming Establishment (GET): The client "upgrades" the session by opening a long-lived GET request to the same endpoint. This creates the "Downstream" (Server $\rightarrow$ Client) pipe.
3. Interaction (POST): The client sends tool calls or resource requests via standard POSTs using the same Session ID.
4. Asynchronous Delivery: The server pushes the results or notifications back through the open GET stream.

---
<details>
    <summary>Example</summary>

```sh
# Initialize a session (Terminal 1)
curl -v -X POST http://localhost:8080/mcp \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream, application/json" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'

# (optional) Upgrade to Streaming (Terminal 2)
curl -N -X GET http://localhost:8080/mcp \
  -H "Accept: text/event-stream" \
  -H "Mcp-Session-Id: <returned-session-id>"
    
# List available tools (Terminal 1)
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -H "Mcp-Session-Id: <returned-session-id>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

</details>
