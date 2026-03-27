# Design: Explicit Module Wiring and Cluster Config Refactor

**Date:** 2026-03-27
**Status:** Approved

---

## Problem

Two issues with the current implementation:

1. **Scan-based bean registration.** Both app modules use `@SpringBootApplication(scanBasePackages = "org.o8h.mcp")`, which pulls in everything under that package tree. The boundary between what `opensearch-mcp-core` intentionally provides and what is incidentally on the classpath is invisible and fragile.

2. **List-type cluster config.** `OpenSearchProperties.clusters` is a `List`, which means adding clusters in a second config file (e.g. environment-specific overrides) replaces the entire list rather than merging. The `name` field inside each list entry is the lookup key used by tools, which creates redundancy.

---

## Solution Overview

| Area | Change |
|---|---|
| `@EnableOpensearchMcp` | New annotation in core; `@Import`s `OpenSearchConfig` + `McpToolConfig` |
| Tool classes | Remove `@Service`; declared as `@Bean` in `McpToolConfig` |
| `OpenSearchProperties` | `List<ClusterProperties>` → `Map<String, ClusterProperties>` |
| `ClusterProperties` | Drop `name`/`host`/`scheme`/`port`; add `url` |
| `OpenSearchConfig` | Build `RestClient` per map entry using `url` directly |
| `application.yml` (all modules) | Switch to map-style cluster config |
| `clusterName` tool param | Mark `required = true` explicitly on all tools |
| `ListClustersTool` | New tool returning registered cluster names |

---

## Section 1: `@EnableOpensearchMcp`

A new annotation lives in `opensearch-mcp-core`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({OpenSearchConfig.class, McpToolConfig.class})
public @interface EnableOpensearchMcp {}
```

Both app modules remove `scanBasePackages` and add `@EnableOpensearchMcp`:

```java
@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpHttpApplication { ... }
```

`OpenSearchConfig` and `McpToolConfig` keep `@Configuration` but are no longer scan-visible — they activate only via `@Import`. Nothing in `opensearch-mcp-core` carries a `@Component` stereotype.

### Tool bean declarations

Tool classes lose `@Service`. `McpToolConfig` explicitly declares each tool as a `@Bean`, injecting `Map<String, RestClient> openSearchClients`:

```java
@Configuration
public class McpToolConfig {

    @Bean
    public ClusterHealthTool clusterHealthTool(Map<String, RestClient> openSearchClients) {
        return new ClusterHealthTool(openSearchClients);
    }

    @Bean
    public ListClustersTool listClustersTool(Map<String, RestClient> openSearchClients) {
        return new ListClustersTool(openSearchClients);
    }

    // ... one @Bean per tool ...

    @Bean
    public ToolCallbackProvider allTools(
            ClusterHealthTool clusterHealthTool,
            ClusterStateTool clusterStateTool,
            GetShardsTool getShardsTool,
            GetSegmentsTool getSegmentsTool,
            GetNodesTool getNodesTool,
            GetNodesHotThreadsTool getNodesHotThreadsTool,
            GetAllocationTool getAllocationTool,
            ListClustersTool listClustersTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        clusterHealthTool, clusterStateTool, getShardsTool,
                        getSegmentsTool, getNodesTool, getNodesHotThreadsTool,
                        getAllocationTool, listClustersTool
                )
                .build();
    }
}
```

---

## Section 2: Cluster Config Refactor

### `ClusterProperties`

Remove `name`, `host`, `scheme`, `port`. Add `url`:

```java
@Setter
@Getter
public static class ClusterProperties {
    private String url;                             // e.g. https://localhost:9200
    private String username;
    private String password;
    private boolean sslVerificationDisabled = false;
}
```

### `OpenSearchProperties`

```java
@Setter
@Getter
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {
    private Map<String, ClusterProperties> clusters = new LinkedHashMap<>();
}
```

The map key is the cluster name (e.g. `"local"`). It acts as the lookup key for tool routing and as a human-readable label in config. Adding a cluster in an environment-specific config file merges into the map rather than replacing the list.

### `OpenSearchConfig`

```java
@Bean
public Map<String, RestClient> openSearchClients(OpenSearchProperties properties) {
    return properties.getClusters().entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> buildClient(e.getValue())
            ));
}

private RestClient buildClient(ClusterProperties cluster) {
    RestClient.Builder builder = RestClient.builder()
            .baseUrl(cluster.getUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeCredentials(cluster));

    if (cluster.isSslVerificationDisabled()) {
        builder.requestFactory(buildSslDisabledRequestFactory());
    }
    return builder.build();
}
```

### `application.yml` (map syntax)

```yaml
opensearch:
  clusters:
    local:
      url: https://localhost:9200
      username:
      password:
      ssl-verification-disabled: true
```

---

## Section 3: `ListClustersTool`

New tool that returns all registered cluster names. The AI client calls this first to discover available clusters before invoking operation tools.

```java
public class ListClustersTool {

    private final Map<String, RestClient> clients;

    public ListClustersTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Lists all available OpenSearch cluster names that can be used as the clusterName parameter in other tools.")
    public List<String> listClusters() {
        return new ArrayList<>(clients.keySet());
    }
}
```

---

## Section 4: Tool `clusterName` param

All existing tools change `clusterName` to `required = true` explicitly:

```java
@ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true)
String clusterName
```

---

## Files Changed

### `opensearch-mcp-core`
- `opensearch/OpenSearchProperties.java` — list → map, ClusterProperties simplified
- `opensearch/OpenSearchConfig.java` — iterate map entries, use `url` directly
- `config/McpToolConfig.java` — explicit `@Bean` per tool, include `ListClustersTool`
- `config/EnableOpensearchMcp.java` — new annotation
- `tool/ListClustersTool.java` — new tool
- All existing tool classes — remove `@Service`, update `@ToolParam` to `required = true`
- `src/main/resources/application.yml` — remove (config belongs to app modules)

### `opensearch-mcp-http`
- `OpensearchMcpHttpApplication.java` — drop `scanBasePackages`, add `@EnableOpensearchMcp`
- `src/main/resources/application.yml` — map-style cluster config, absorb core's properties
- `src/test/resources/application.yml` — map-style cluster config

### `opensearch-mcp-stdio`
- `OpensearchMcpStdioApplication.java` — drop `scanBasePackages`, add `@EnableOpensearchMcp`
- `src/main/resources/application.yml` — map-style cluster config, absorb core's properties (create if not present)
