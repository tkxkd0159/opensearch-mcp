# Dual-Mode OpenSearch MCP Server

**Date:** 2026-04-02  
**Status:** Approved

## Summary

Extend `opensearch-mcp-http` to support two access modes in a single server:

1. **Registered mode** — caller provides a `clusterName`; server looks up a pre-built `RestClient` from statically configured clusters in `application.yml`.
2. **Ad-hoc (proxy) mode** — caller provides a `clusterUrl` tool parameter; server reads credentials from request headers and builds a dynamic `RestClient` on the fly.

## Motivation

The current server only serves pre-registered clusters. Deploying it as a remote MCP server is impractical because operators cannot anticipate every cluster a caller might want to target. Ad-hoc mode removes this restriction for HTTP deployments while keeping registered mode for controlled environments.

## Modes

| Mode | Trigger | Credentials source |
|---|---|---|
| Registered | `clusterName` param set, `clusterUrl` absent | Pre-built `RestClient` from `application.yml` |
| Ad-hoc | `clusterUrl` param set | `X-OpenSearch-Username`, `X-OpenSearch-Password`, `X-OpenSearch-SSL-Disabled` headers |

Ad-hoc mode is only meaningful for the HTTP transport (`opensearch-mcp-http`). stdio users register clusters locally and use registered mode.

## Architecture

A new `ClusterResolver` service in `opensearch-mcp-core` encapsulates all resolution logic. Tools call `clusterResolver.resolve(clusterName, clusterUrl)` and receive a ready `RestClient`. No resolution logic lives in individual tools.

## Components

### `ClusterResolver` (new, `opensearch-mcp-core`)

```
ClusterResolver
  - Map<String, RestClient> registeredClients   (injected)
  + RestClient resolve(String clusterName, String clusterUrl)
```

Resolution rules (in order):
1. If `clusterUrl` is non-null → read `X-OpenSearch-Username`, `X-OpenSearch-Password`, `X-OpenSearch-SSL-Disabled` from `RequestContextHolder`; build a fresh `RestClient` targeting `clusterUrl`. Not cached — credentials may differ per session.
2. Else if `clusterName` is non-null → look up in `registeredClients`; return error string if unknown.
3. Else → return error string (both null).

`X-OpenSearch-SSL-Disabled` defaults to `false` (SSL verified) when the header is absent.

### Tools (all 8, `opensearch-mcp-core`)

Each tool gains one new optional `@ToolParam`:

```java
@ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). "
    + "Use this for ad-hoc access without pre-registration. "
    + "Requires X-OpenSearch-Username and X-OpenSearch-Password headers to be set on the MCP client.", 
    required = false) String clusterUrl
```

`clusterName` is updated to `required = false`.

The lookup line `clients.get(clusterName)` is replaced with `clusterResolver.resolve(clusterName, clusterUrl)`.

### `McpToolConfig` (`opensearch-mcp-core`)

- New `ClusterResolver` bean wired from `openSearchClients` map.
- All tool beans receive `ClusterResolver` instead of the raw `Map<String, RestClient>`.

## Request Headers (Ad-hoc Mode)

| Header | Required | Description |
|---|---|---|
| `X-OpenSearch-Username` | Yes | Basic auth username |
| `X-OpenSearch-Password` | Yes | Basic auth password |
| `X-OpenSearch-SSL-Disabled` | No | `true` to skip SSL verification. Default: `false` |

Set these in the MCP client config:

```json
{
  "mcpServers": {
    "opensearch-adhoc": {
      "url": "http://my-mcp-server:8080/mcp",
      "headers": {
        "X-OpenSearch-Username": "admin",
        "X-OpenSearch-Password": "secret",
        "X-OpenSearch-SSL-Disabled": "true"
      }
    }
  }
}
```

The AI then calls tools with the `clusterUrl` parameter:

```
getClusterHealth(clusterUrl="https://my-cluster:9200")
```

## Data Flow

**Registered mode:**
```
MCP Client → POST /mcp  { clusterName="prod" }
  → Tool → ClusterResolver.resolve("prod", null)
  → Map lookup → pre-built RestClient
  → OpenSearch cluster
```

**Ad-hoc mode:**
```
MCP Client → POST /mcp  { clusterUrl="https://my-cluster:9200" }
             + X-OpenSearch-Username / X-OpenSearch-Password / X-OpenSearch-SSL-Disabled
  → Tool → ClusterResolver.resolve(null, "https://my-cluster:9200")
  → Read headers from RequestContextHolder
  → Build fresh RestClient
  → OpenSearch cluster
```

## Error Handling

All errors are returned as plain strings (consistent with existing tool error responses).

| Condition | Response |
|---|---|
| `clusterUrl` set, but `X-OpenSearch-Username` or `X-OpenSearch-Password` missing | `"Ad-hoc mode requires X-OpenSearch-Username and X-OpenSearch-Password headers."` |
| `clusterName` not found in registered map | `"Unknown cluster: <name>. Available clusters: [...]"` (existing behavior) |
| Both `clusterName` and `clusterUrl` null | `"Either clusterName or clusterUrl must be provided."` |
| SSL client build failure | Propagated as error string |

## Security Considerations

Ad-hoc mode directs the server to make HTTP requests to any URL provided by the LLM via `clusterUrl`. This is intentional SSRF-by-design. Operators must ensure the MCP server endpoint is only reachable from trusted clients when ad-hoc mode is in use.

## Testing

- **Unit:** `ClusterResolverTest` — registered mode, ad-hoc mode, missing headers, both-null, SSL-disabled header parsing.
- **Existing tool tests** — unaffected; they pass `clusterName`, `clusterUrl=null`.
- **Integration:** Add one ad-hoc scenario in `ToolsIntegrationTest` — set mock request headers, pass `clusterUrl`, verify the tool targets the correct cluster.
