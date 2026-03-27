# Module Wiring and Cluster Config Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace scan-based bean registration with explicit `@EnableOpensearchMcp`, and refactor cluster config from a `List` to a `Map<name, ClusterProperties>` with a `url` field.

**Architecture:** `opensearch-mcp-core` gains an `@EnableOpensearchMcp` annotation that `@Import`s `OpenSearchConfig` and `McpToolConfig` explicitly. All tool classes lose `@Service` and are declared as `@Bean`s in `McpToolConfig`. `OpenSearchProperties` changes its `clusters` field from `List<ClusterProperties>` to `Map<String, ClusterProperties>`, where the key is the cluster name and `ClusterProperties` replaces `host`/`scheme`/`port` with a single `url` field.

**Tech Stack:** Spring Boot 4.0.5, Spring AI 2.0.0-M3, Java 25, Gradle (Kotlin DSL), JUnit 5, AssertJ, Spring `MockRestServiceServer`

---

## File Map

| Action | File |
|---|---|
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchProperties.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchConfig.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java` |
| Create | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/EnableOpensearchMcp.java` |
| Create | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ListClustersTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterHealthTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterStateTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetShardsTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetSegmentsTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesHotThreadsTool.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetAllocationTool.java` |
| Create | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/OpenSearchPropertiesTest.java` |
| Create | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ListClustersToolTest.java` |
| Modify | `opensearch-mcp-http/src/main/java/org/o8h/mcp/http/OpensearchMcpHttpApplication.java` |
| Modify | `opensearch-mcp-http/src/main/resources/application.yml` |
| Modify | `opensearch-mcp-http/src/test/resources/application.yml` |
| Modify | `opensearch-mcp-stdio/src/main/java/org/o8h/mcp/stdio/OpensearchMcpStdioApplication.java` |
| Modify | `opensearch-mcp-stdio/src/main/resources/application.yml` |

---

### Task 1: Refactor `OpenSearchProperties` — list → map

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchProperties.java`
- Create: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/OpenSearchPropertiesTest.java`

- [ ] **Step 1: Write the failing test**

```java
// opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/OpenSearchPropertiesTest.java
package org.o8h.mcp.core.opensearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(OpenSearchProperties.class)
    static class TestConfig {}

    @Test
    void clusters_mapBinding_populatesFromProperties() {
        runner.withPropertyValues(
                "opensearch.clusters.local.url=https://localhost:9200",
                "opensearch.clusters.local.username=admin",
                "opensearch.clusters.local.password=secret",
                "opensearch.clusters.local.ssl-verification-disabled=true"
        ).run(ctx -> {
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).containsKey("local");
            OpenSearchProperties.ClusterProperties local = props.getClusters().get("local");
            assertThat(local.getUrl()).isEqualTo("https://localhost:9200");
            assertThat(local.getUsername()).isEqualTo("admin");
            assertThat(local.getPassword()).isEqualTo("secret");
            assertThat(local.isSslVerificationDisabled()).isTrue();
        });
    }

    @Test
    void clusters_multipleClusters_allBound() {
        runner.withPropertyValues(
                "opensearch.clusters.local.url=https://localhost:9200",
                "opensearch.clusters.prod.url=https://prod.example.com:9200"
        ).run(ctx -> {
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).containsKeys("local", "prod");
        });
    }

    @Test
    void clusters_empty_returnsEmptyMap() {
        runner.run(ctx -> {
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).isEmpty();
        });
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.opensearch.OpenSearchPropertiesTest" 2>&1 | tail -20
```

Expected: FAIL — `getClusters()` returns a `List`, not a `Map`.

- [ ] **Step 3: Replace `OpenSearchProperties.java` with map-based implementation**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchProperties.java
package org.o8h.mcp.core.opensearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

    private Map<String, ClusterProperties> clusters = new LinkedHashMap<>();

    @Setter
    @Getter
    public static class ClusterProperties {
        private String url;
        private String username;
        private String password;
        private boolean sslVerificationDisabled = false;
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.opensearch.OpenSearchPropertiesTest" 2>&1 | tail -20
```

Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchProperties.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/OpenSearchPropertiesTest.java
git commit -m "refactor: change cluster config from List to Map with url field"
```

---

### Task 2: Refactor `OpenSearchConfig` — use map entries and url

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchConfig.java`

- [ ] **Step 1: Replace `OpenSearchConfig.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchConfig.java
package org.o8h.mcp.core.opensearch;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    @Bean
    public Map<String, RestClient> openSearchClients(OpenSearchProperties properties) {
        return properties.getClusters().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> buildClient(e.getValue())
                ));
    }

    private RestClient buildClient(OpenSearchProperties.ClusterProperties cluster) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(cluster.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeCredentials(cluster));

        if (cluster.isSslVerificationDisabled()) {
            builder.requestFactory(buildSslDisabledRequestFactory());
        }

        return builder.build();
    }

    private String encodeCredentials(OpenSearchProperties.ClusterProperties cluster) {
        return Base64.getEncoder().encodeToString(
                (cluster.getUsername() + ":" + cluster.getPassword()).getBytes());
    }

    private HttpComponentsClientHttpRequestFactory buildSslDisabledRequestFactory() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (cert, authType) -> true)
                    .build();

            var tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);

            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(tlsStrategy)
                    .build();

            var httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new IllegalStateException("Failed to create SSL-disabled HTTP client", e);
        }
    }
}
```

- [ ] **Step 2: Run core tests to verify no regressions**

```bash
./gradlew :opensearch-mcp-core:test 2>&1 | tail -20
```

Expected: PASS — existing tool unit tests still pass (they construct `RestClient` directly, not via config).

- [ ] **Step 3: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/OpenSearchConfig.java
git commit -m "refactor: update OpenSearchConfig to build clients from map entries using url"
```

---

### Task 3: Add `ListClustersTool`

**Files:**
- Create: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ListClustersTool.java`
- Create: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ListClustersToolTest.java`

- [ ] **Step 1: Write the failing test**

```java
// opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ListClustersToolTest.java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.OpenSearchProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListClustersToolTest {

    @Test
    void listClusters_returnsNameAndUrlPerCluster() {
        OpenSearchProperties props = new OpenSearchProperties();
        OpenSearchProperties.ClusterProperties local = new OpenSearchProperties.ClusterProperties();
        local.setUrl("https://localhost:9200");
        local.setUsername("admin");
        local.setPassword("secret");
        props.getClusters().put("local", local);

        OpenSearchProperties.ClusterProperties prod = new OpenSearchProperties.ClusterProperties();
        prod.setUrl("https://prod.example.com:9200");
        props.getClusters().put("prod", prod);

        ListClustersTool tool = new ListClustersTool(props);
        List<Map<String, String>> result = tool.listClusters();

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entry -> {
            assertThat(entry).containsEntry("name", "local");
            assertThat(entry).containsEntry("url", "https://localhost:9200");
            assertThat(entry).doesNotContainKey("username");
            assertThat(entry).doesNotContainKey("password");
        });
        assertThat(result).anySatisfy(entry -> {
            assertThat(entry).containsEntry("name", "prod");
            assertThat(entry).containsEntry("url", "https://prod.example.com:9200");
        });
    }

    @Test
    void listClusters_emptyClusters_returnsEmptyList() {
        ListClustersTool tool = new ListClustersTool(new OpenSearchProperties());
        assertThat(tool.listClusters()).isEmpty();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ListClustersToolTest" 2>&1 | tail -20
```

Expected: FAIL — `ListClustersTool` does not exist yet.

- [ ] **Step 3: Create `ListClustersTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ListClustersTool.java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public class ListClustersTool {

    private final OpenSearchProperties properties;

    public ListClustersTool(OpenSearchProperties properties) {
        this.properties = properties;
    }

    @Tool(description = "Lists all available OpenSearch clusters with their name and URL. Use the name as the clusterName parameter in other tools.")
    public List<Map<String, String>> listClusters() {
        return properties.getClusters().entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "url", e.getValue().getUrl()))
                .toList();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ListClustersToolTest" 2>&1 | tail -20
```

Expected: PASS — both tests green.

- [ ] **Step 5: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ListClustersTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ListClustersToolTest.java
git commit -m "feat: add ListClustersTool returning name and url per cluster"
```

---

### Task 4: Remove `@Service` from tool classes and update `@ToolParam`

**Files:**
- Modify: all 7 tool classes in `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/`

- [ ] **Step 1: Update `ClusterHealthTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterHealthTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class ClusterHealthTool {

    private final Map<String, RestClient> clients;

    public ClusterHealthTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
    public String getClusterHealth(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.", required = false) String index
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(index))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cluster/health";
        }
        return "/_cluster/health/" + index;
    }
}
```

- [ ] **Step 2: Update `ClusterStateTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterStateTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class ClusterStateTool {

    private final Map<String, RestClient> clients;

    public ClusterStateTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets the current state of an OpenSearch cluster including node information, index metadata, shard routing, and blocks. Metrics can be filtered to: nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid. Indices can be filtered by name or wildcard.")
    public String getClusterState(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Comma-separated metrics to retrieve (nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid). Omit for all metrics.", required = false) String metrics,
            @ToolParam(description = "Comma-separated index names or wildcards to filter. Omit for all indices.", required = false) String indices
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(metrics, indices))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String metrics, String indices) {
        if (metrics == null || metrics.isBlank()) {
            return "/_cluster/state";
        }
        if (indices == null || indices.isBlank()) {
            return "/_cluster/state/" + metrics;
        }
        return "/_cluster/state/" + metrics + "/" + indices;
    }
}
```

- [ ] **Step 3: Update `GetShardsTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetShardsTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetShardsTool {

    private final Map<String, RestClient> clients;

    public GetShardsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
    public String getShards(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to filter shards. Omit for all shards.", required = false) String index
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(index) + "?v=true&format=json")
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/shards";
        }
        return "/_cat/shards/" + index;
    }
}
```

- [ ] **Step 4: Update `GetSegmentsTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetSegmentsTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetSegmentsTool {

    private final Map<String, RestClient> clients;

    public GetSegmentsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about Lucene segments in OpenSearch indices, including memory usage, document counts, segment sizes, and whether segments are committed or searchable.")
    public String getSegments(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to filter segments. Omit for all indices.", required = false) String index
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(index) + "?v=true&format=json")
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/segments";
        }
        return "/_cat/segments/" + index;
    }
}
```

- [ ] **Step 5: Update `GetNodesTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetNodesTool {

    private final Map<String, RestClient> clients;

    public GetNodesTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets detailed information about nodes in an OpenSearch cluster, including static information like host system details, JVM info, processor type, node settings, thread pools, and installed plugins. Metrics can be filtered to categories like: settings, os, process, jvm, thread_pool, transport, http, plugins, ingest.")
    public String getNodes(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId,
            @ToolParam(description = "Comma-separated metrics categories to retrieve (e.g. settings, os, process, jvm, thread_pool, transport, http, plugins, ingest). Omit for all metrics.", required = false) String metrics
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId, metrics))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId, String metrics) {
        boolean hasNodeId = nodeId != null && !nodeId.isBlank();
        boolean hasMetrics = metrics != null && !metrics.isBlank();

        if (!hasNodeId && !hasMetrics) return "/_nodes";
        if (hasNodeId && !hasMetrics) return "/_nodes/" + nodeId;
        if (!hasNodeId) return "/_nodes/" + metrics;
        return "/_nodes/" + nodeId + "/" + metrics;
    }
}
```

- [ ] **Step 6: Update `GetNodesHotThreadsTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesHotThreadsTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetNodesHotThreadsTool {

    private final Map<String, RestClient> clients;

    public GetNodesHotThreadsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
    public String getNodesHotThreads(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_nodes/hot_threads";
        }
        return "/_nodes/" + nodeId + "/hot_threads";
    }
}
```

- [ ] **Step 7: Update `GetAllocationTool.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetAllocationTool.java
package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetAllocationTool {

    private final Map<String, RestClient> clients;

    public GetAllocationTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
    public String getAllocation(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Node ID or name to filter allocation info for a specific node. Omit for all nodes.", required = false) String nodeId
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId) + "?v=true&format=json")
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_cat/allocation";
        }
        return "/_cat/allocation/" + nodeId;
    }
}
```

- [ ] **Step 8: Run all core unit tests to verify no regressions**

```bash
./gradlew :opensearch-mcp-core:test 2>&1 | tail -30
```

Expected: PASS — all existing tool unit tests still pass because they construct tools with `new ClusterHealthTool(Map.of(...))` directly.

- [ ] **Step 9: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/
git commit -m "refactor: remove @Service from tools, add required=true to clusterName params"
```

---

### Task 5: Update `McpToolConfig` — explicit `@Bean` per tool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java`

- [ ] **Step 1: Replace `McpToolConfig.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java
package org.o8h.mcp.core.config;

import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.o8h.mcp.core.tool.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class McpToolConfig {

    @Bean
    public ClusterHealthTool clusterHealthTool(Map<String, RestClient> openSearchClients) {
        return new ClusterHealthTool(openSearchClients);
    }

    @Bean
    public ClusterStateTool clusterStateTool(Map<String, RestClient> openSearchClients) {
        return new ClusterStateTool(openSearchClients);
    }

    @Bean
    public GetShardsTool getShardsTool(Map<String, RestClient> openSearchClients) {
        return new GetShardsTool(openSearchClients);
    }

    @Bean
    public GetSegmentsTool getSegmentsTool(Map<String, RestClient> openSearchClients) {
        return new GetSegmentsTool(openSearchClients);
    }

    @Bean
    public GetNodesTool getNodesTool(Map<String, RestClient> openSearchClients) {
        return new GetNodesTool(openSearchClients);
    }

    @Bean
    public GetNodesHotThreadsTool getNodesHotThreadsTool(Map<String, RestClient> openSearchClients) {
        return new GetNodesHotThreadsTool(openSearchClients);
    }

    @Bean
    public GetAllocationTool getAllocationTool(Map<String, RestClient> openSearchClients) {
        return new GetAllocationTool(openSearchClients);
    }

    @Bean
    public ListClustersTool listClustersTool(OpenSearchProperties properties) {
        return new ListClustersTool(properties);
    }

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

- [ ] **Step 2: Run all core tests**

```bash
./gradlew :opensearch-mcp-core:test 2>&1 | tail -20
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java
git commit -m "refactor: declare all tools as explicit @Bean in McpToolConfig"
```

---

### Task 6: Add `@EnableOpensearchMcp` annotation

**Files:**
- Create: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/EnableOpensearchMcp.java`

- [ ] **Step 1: Create `EnableOpensearchMcp.java`**

```java
// opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/EnableOpensearchMcp.java
package org.o8h.mcp.core.config;

import org.o8h.mcp.core.opensearch.OpenSearchConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({OpenSearchConfig.class, McpToolConfig.class})
public @interface EnableOpensearchMcp {
}
```

- [ ] **Step 2: Run core tests to verify nothing broken**

```bash
./gradlew :opensearch-mcp-core:test 2>&1 | tail -20
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/EnableOpensearchMcp.java
git commit -m "feat: add @EnableOpensearchMcp annotation importing OpenSearchConfig and McpToolConfig"
```

---

### Task 7: Update app modules — adopt `@EnableOpensearchMcp` and map-style yaml

**Files:**
- Modify: `opensearch-mcp-http/src/main/java/org/o8h/mcp/http/OpensearchMcpHttpApplication.java`
- Modify: `opensearch-mcp-http/src/main/resources/application.yml`
- Modify: `opensearch-mcp-http/src/test/resources/application.yml`
- Modify: `opensearch-mcp-stdio/src/main/java/org/o8h/mcp/stdio/OpensearchMcpStdioApplication.java`
- Modify: `opensearch-mcp-stdio/src/main/resources/application.yml`

- [ ] **Step 1: Update `OpensearchMcpHttpApplication.java`**

```java
// opensearch-mcp-http/src/main/java/org/o8h/mcp/http/OpensearchMcpHttpApplication.java
package org.o8h.mcp.http;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpHttpApplication.class, args);
    }
}
```

- [ ] **Step 2: Update `opensearch-mcp-http/src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: opensearch-mcp
  ai:
    mcp:
      server:
        name: o8h-mcp-server
        protocol: STREAMABLE

server:
  port: 8080

management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: "*"

opensearch:
  clusters:
    local:
      url: https://localhost:9200
      username:
      password:
      ssl-verification-disabled: true
```

- [ ] **Step 3: Update `opensearch-mcp-http/src/test/resources/application.yml`**

```yaml
opensearch:
  clusters:
    local:
      url: https://localhost:9200
      username:
      password:
      ssl-verification-disabled: true
```

- [ ] **Step 4: Update `OpensearchMcpStdioApplication.java`**

```java
// opensearch-mcp-stdio/src/main/java/org/o8h/mcp/stdio/OpensearchMcpStdioApplication.java
package org.o8h.mcp.stdio;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpStdioApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpStdioApplication.class, args);
    }
}
```

- [ ] **Step 5: Update `opensearch-mcp-stdio/src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: opensearch-mcp
  main:
    web-application-type: none
  ai:
    mcp:
      server:
        name: o8h-mcp-server
        stdio: true

opensearch:
  clusters:
    local:
      url: https://localhost:9200
      username: ${OPENSEARCH_USERNAME:admin}
      password: ${OPENSEARCH_PASSWORD:}
      ssl-verification-disabled: true
```

Note: the stdio yml previously used `${TEST_USER:admin}` and `${TEST_PW}` — updated to conventional Spring env var names `OPENSEARCH_USERNAME` and `OPENSEARCH_PASSWORD`. Adjust if your deployment uses different variable names.

- [ ] **Step 6: Commit**

```bash
git add opensearch-mcp-http/src/main/java/org/o8h/mcp/http/OpensearchMcpHttpApplication.java \
        opensearch-mcp-http/src/main/resources/application.yml \
        opensearch-mcp-http/src/test/resources/application.yml \
        opensearch-mcp-stdio/src/main/java/org/o8h/mcp/stdio/OpensearchMcpStdioApplication.java \
        opensearch-mcp-stdio/src/main/resources/application.yml
git commit -m "refactor: adopt @EnableOpensearchMcp and map-style cluster config in app modules"
```

---

### Task 8: Full build verification

- [ ] **Step 1: Run the full build**

```bash
./gradlew clean build 2>&1 | tail -40
```

Expected: `BUILD SUCCESSFUL`. The `opensearch-mcp-core` unit tests pass. The `OpensearchMcpHttpApplicationTests.contextLoads()` test passes (context wires up via `@EnableOpensearchMcp`). The `ToolsIntegrationTest` tests require a running OpenSearch at `https://localhost:9200` — if none is available they will fail with `Unauthorized` or connection errors, which is expected in environments without OpenSearch. Core unit tests should always be green.

- [ ] **Step 2: Verify `contextLoads` specifically**

```bash
./gradlew :opensearch-mcp-http:test --tests "org.o8h.mcp.http.OpensearchMcpHttpApplicationTests" 2>&1 | tail -20
```

Expected: PASS — context loads cleanly without NPE.

- [ ] **Step 3: Commit if anything was adjusted during verification**

```bash
git add -p   # stage only intentional fixes
git commit -m "fix: address any issues found during build verification"
```
