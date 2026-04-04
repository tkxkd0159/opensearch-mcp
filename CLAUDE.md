# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
# :<subproject-directory-name>:<task-name>

# Build fat JARs (outputs to build/libs/)
./gradlew :opensearch-mcp-http:bootJar :opensearch-mcp-stdio:bootJar

# Run all tests
./gradlew test

# Run tests for a single module
./gradlew :opensearch-mcp-core:test
./gradlew :opensearch-mcp-http:test

# Run a single test class
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ClusterHealthToolTest"
```

## Module Architecture

Three Gradle modules:

- **opensearch-mcp-core** — shared library containing all MCP tools, OpenSearch client config, and `ClusterResolver`. No `main()` class.
- **opensearch-mcp-http** — Spring Boot app (port 8080) using Streamable HTTP transport. Imports core via `@EnableOpensearchMcp`.
- **opensearch-mcp-stdio** — Spring Boot app (no web server) using stdio transport. Imports core via `@EnableOpensearchMcp`.

Build conventions (Java version, test settings) live in `build-logic/src/main/kotlin/org.o8h.java-conventions.gradle.kts`.

## Key Wiring: How Tools Get Registered

`@EnableOpensearchMcp` (in `opensearch-mcp-core`) is a meta-annotation that imports `McpToolConfig`. That config class is the single factory for all tool beans and assembles a `ToolCallbackProvider` that Spring AI's MCP server picks up automatically. Adding a new tool means: create the tool class, declare it as a bean in `McpToolConfig`.

## Dual-Mode Cluster Resolution

`ClusterResolver` supports two modes:

1. **Registered clusters** — pre-configured in `application.yml` under `opensearch.clusters.<name>`. Tools receive a cluster name string, and `ClusterResolver` maps it to a `RestClient`.
2. **Ad-hoc clusters** — tools pass a URL directly; credentials come from HTTP headers (`X-OpenSearch-Username`, `X-OpenSearch-Password`, `X-OpenSearch-SSL-Disabled`). A fresh `RestClient` is built per-request.

Both modes converge on `ClusterResolver.resolve(...)` returning a `RestClient`.

## Configuration Shape

```yaml
opensearch:
  write-enabled: false        # gates POST/PUT/DELETE/PATCH in GenericOpenSearchApiTool
  clusters:
    my-cluster:
      url: https://localhost:9200
      username: admin
      password: ${OPENSEARCH_PASSWORD:}
      ssl-verification-disabled: true
```

Local overrides go in `application-local.yml` (git-ignored). Activate with `--spring.profiles.active=local`.

## MCP Tools Inventory

All tools live in `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/`:

| Tool                       | Purpose                                                |
|----------------------------|--------------------------------------------------------|
| `ListClustersTool`         | Lists registered cluster names                         |
| `ClusterHealthTool`        | Cluster/index health                                   |
| `ClusterStateTool`         | Full cluster state                                     |
| `GetShardsTool`            | Shard allocation info                                  |
| `GetSegmentsTool`          | Segment stats                                          |
| `GetNodesTool`             | Node info with optional metrics filter                 |
| `GetNodesHotThreadsTool`   | Hot thread stacks                                      |
| `GetAllocationTool`        | Shard allocation explanation                           |
| `GenericOpenSearchApiTool` | Any OpenSearch endpoint; respects `write-enabled` flag |

## HTTP Transport Endpoints

```
POST   /mcp    Initialize session / send requests
GET    /mcp    Open SSE stream (server → client)
DELETE /mcp    Terminate session
```

Actuator runs on port 8081.

## Testing Patterns

Unit tests mock `RestClient` using `MockRestServiceServer`. See `ClusterHealthToolTest` for the canonical pattern. The test `application.yml` in each module's `src/test/resources/` defines a dummy cluster so `OpenSearchConfig` initializes without a live server.

Integration tests (`ToolsIntegrationTest`) require a running OpenSearch instance or explicit mocking at the HTTP level.
