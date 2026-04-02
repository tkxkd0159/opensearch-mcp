# Dual-Mode Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ad-hoc cluster access to the OpenSearch MCP server so tools can target any cluster URL using credentials passed as request headers, while preserving the existing registered-cluster flow.

**Architecture:** A new `ClusterResolver` service encapsulates the decision: if `clusterUrl` is provided as a tool parameter, read `X-OpenSearch-Username`/`X-OpenSearch-Password`/`X-OpenSearch-SSL-Disabled` from the HTTP request headers and build a fresh `RestClient`; otherwise look up the pre-built client by `clusterName`. All 7 tools that call OpenSearch swap their `Map<String, RestClient>` field for `ClusterResolver`. `McpToolConfig` wires the new bean.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI 2.0.0-M3, Spring Web (`RestClient`, `RequestContextHolder`), JUnit 5, AssertJ, `spring-test` (`MockHttpServletRequest`, `MockRestServiceServer`)

---

## File Map

| Action | Path |
|--------|------|
| Create | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/ClusterResolver.java` |
| Create | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/ClusterResolverTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterHealthTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterHealthToolTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterStateTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterStateToolTest.java` (create if absent) |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetShardsTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetShardsToolTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetSegmentsTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetSegmentsToolTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesToolTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesHotThreadsTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesHotThreadsToolTest.java` |
| Modify | `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetAllocationTool.java` |
| Modify | `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetAllocationToolTest.java` |
| Modify | `opensearch-mcp-http/src/test/java/org/o8h/mcp/http/tool/ToolsIntegrationTest.java` |

---

### Task 1: ClusterResolver — failing tests

**Files:**
- Create: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/ClusterResolverTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package org.o8h.mcp.core.opensearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClusterResolverTest {

    private final RestClient registeredClient = mock(RestClient.class);
    private final ClusterResolver resolver = new ClusterResolver(Map.of("prod", registeredClient));

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolve_withClusterName_returnsRegisteredClient() {
        assertThat(resolver.resolve("prod", null)).isSameAs(registeredClient);
    }

    @Test
    void resolve_withUnknownClusterName_throwsWithMessage() {
        assertThatThrownBy(() -> resolver.resolve("unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown cluster: unknown")
                .hasMessageContaining("prod");
    }

    @Test
    void resolve_bothNull_throws() {
        assertThatThrownBy(() -> resolver.resolve(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either clusterName or clusterUrl must be provided");
    }

    @Test
    void resolve_withClusterUrl_andValidHeaders_returnsNewClient() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient result = resolver.resolve(null, "http://my-cluster:9200");

        assertThat(result).isNotNull().isNotSameAs(registeredClient);
    }

    @Test
    void resolve_withClusterUrl_missingUsername_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-OpenSearch-Username");
    }

    @Test
    void resolve_withClusterUrl_missingPassword_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-OpenSearch-Password");
    }

    @Test
    void resolve_withClusterUrl_noRequestContext_throws() {
        // No request context set — simulates stdio transport
        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supported over HTTP transport");
    }

    @Test
    void resolve_clusterUrlTakesPrecedenceOverClusterName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient result = resolver.resolve("prod", "http://other-cluster:9200");

        assertThat(result).isNotSameAs(registeredClient);
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure** (class does not exist yet)

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.opensearch.ClusterResolverTest"
```

Expected: BUILD FAILED — `ClusterResolver` not found.

---

### Task 2: ClusterResolver — implementation

**Files:**
- Create: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/ClusterResolver.java`

- [ ] **Step 1: Implement ClusterResolver**

```java
package org.o8h.mcp.core.opensearch;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class ClusterResolver {

    private static final String HEADER_USERNAME = "X-OpenSearch-Username";
    private static final String HEADER_PASSWORD = "X-OpenSearch-Password";
    private static final String HEADER_SSL_DISABLED = "X-OpenSearch-SSL-Disabled";

    private final Map<String, RestClient> registeredClients;

    public ClusterResolver(Map<String, RestClient> registeredClients) {
        this.registeredClients = registeredClients;
    }

    public RestClient resolve(String clusterName, String clusterUrl) {
        if (clusterUrl != null && !clusterUrl.isBlank()) {
            return buildAdHocClient(clusterUrl);
        }
        if (clusterName != null && !clusterName.isBlank()) {
            RestClient client = registeredClients.get(clusterName);
            if (client == null) {
                throw new IllegalArgumentException(
                        "Unknown cluster: " + clusterName + ". Available clusters: " + registeredClients.keySet());
            }
            return client;
        }
        throw new IllegalArgumentException("Either clusterName or clusterUrl must be provided.");
    }

    private RestClient buildAdHocClient(String clusterUrl) {
        var requestAttrs = RequestContextHolder.getRequestAttributes();
        if (!(requestAttrs instanceof ServletRequestAttributes)) {
            throw new IllegalArgumentException(
                    "Ad-hoc mode (clusterUrl) is only supported over HTTP transport.");
        }
        var request = ((ServletRequestAttributes) requestAttrs).getRequest();

        String username = request.getHeader(HEADER_USERNAME);
        String password = request.getHeader(HEADER_PASSWORD);

        if (username == null || password == null) {
            throw new IllegalArgumentException(
                    "Ad-hoc mode requires X-OpenSearch-Username and X-OpenSearch-Password headers.");
        }

        boolean sslDisabled = "true".equalsIgnoreCase(request.getHeader(HEADER_SSL_DISABLED));
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(clusterUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);

        if (sslDisabled) {
            builder.requestFactory(buildSslDisabledRequestFactory());
        }

        return builder.build();
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

> **Note:** If `jakarta.net.ssl.SSLContext` does not compile, use `javax.net.ssl.SSLContext` instead — the existing `OpenSearchConfig.java` in this module uses `javax.net.ssl.SSLContext`; match whatever it imports.

- [ ] **Step 2: Run tests — expect green**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.opensearch.ClusterResolverTest"
```

Expected: BUILD SUCCESSFUL, 7 tests passed.

- [ ] **Step 3: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/opensearch/ClusterResolver.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/opensearch/ClusterResolverTest.java
git commit -m "feat: add ClusterResolver for dual-mode cluster access"
```

---

### Task 3: Wire ClusterResolver in McpToolConfig

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java`

- [ ] **Step 1: Replace the file content**

Replace the entire file with:

```java
package org.o8h.mcp.core.config;

import org.o8h.mcp.core.opensearch.ClusterResolver;
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
    public ClusterResolver clusterResolver(Map<String, RestClient> openSearchClients) {
        return new ClusterResolver(openSearchClients);
    }

    @Bean
    public ClusterHealthTool clusterHealthTool(ClusterResolver clusterResolver) {
        return new ClusterHealthTool(clusterResolver);
    }

    @Bean
    public ClusterStateTool clusterStateTool(ClusterResolver clusterResolver) {
        return new ClusterStateTool(clusterResolver);
    }

    @Bean
    public GetShardsTool getShardsTool(ClusterResolver clusterResolver) {
        return new GetShardsTool(clusterResolver);
    }

    @Bean
    public GetSegmentsTool getSegmentsTool(ClusterResolver clusterResolver) {
        return new GetSegmentsTool(clusterResolver);
    }

    @Bean
    public GetNodesTool getNodesTool(ClusterResolver clusterResolver) {
        return new GetNodesTool(clusterResolver);
    }

    @Bean
    public GetNodesHotThreadsTool getNodesHotThreadsTool(ClusterResolver clusterResolver) {
        return new GetNodesHotThreadsTool(clusterResolver);
    }

    @Bean
    public GetAllocationTool getAllocationTool(ClusterResolver clusterResolver) {
        return new GetAllocationTool(clusterResolver);
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

- [ ] **Step 2: Compile only — tools not updated yet, expect compilation errors on tool constructors**

```bash
./gradlew :opensearch-mcp-core:compileJava
```

Expected: errors like `ClusterHealthTool(Map<String, RestClient>)` — that's expected; tools are updated in the next tasks.

- [ ] **Step 3: Commit (even with compile errors)**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/config/McpToolConfig.java
git commit -m "refactor: wire ClusterResolver into McpToolConfig"
```

---

### Task 4: Update ClusterHealthTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterHealthTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterHealthToolTest.java`

- [ ] **Step 1: Replace ClusterHealthTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ClusterHealthTool {

    private final ClusterResolver clusterResolver;

    public ClusterHealthTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
    public String getClusterHealth(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.", required = false) String index
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(index))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cluster/health";
        }
        return "/_cluster/health/" + index;
    }
}
```

- [ ] **Step 2: Replace ClusterHealthToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClusterHealthToolTest {

    private MockRestServiceServer mockServer;
    private ClusterHealthTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new ClusterHealthTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getClusterHealth_noIndex_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"green\"}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterHealth("local", null, null);

        mockServer.verify();
        assertThat(result).contains("green");
    }

    @Test
    void getClusterHealth_withIndex_callsIndexPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/health/my-index"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"yellow\"}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterHealth("local", null, "my-index");

        mockServer.verify();
        assertThat(result).contains("yellow");
    }

    @Test
    void getClusterHealth_unknownCluster_returnsError() {
        String result = tool.getClusterHealth("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }

    @Test
    void getClusterHealth_bothNull_returnsError() {
        String result = tool.getClusterHealth(null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ClusterHealthToolTest"
```

Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterHealthTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterHealthToolTest.java
git commit -m "feat: add clusterUrl support to ClusterHealthTool"
```

---

### Task 5: Update ClusterStateTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterStateTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterStateToolTest.java`

- [ ] **Step 1: Replace ClusterStateTool** (`ClusterStateToolTest` does not yet exist — create it in Step 2)

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ClusterStateTool {

    private final ClusterResolver clusterResolver;

    public ClusterStateTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets the current state of an OpenSearch cluster including node information, index metadata, shard routing, and blocks. Metrics can be filtered to: nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid. Indices can be filtered by name or wildcard.")
    public String getClusterState(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Comma-separated metrics to retrieve (nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid). Omit for all metrics.", required = false) String metrics,
            @ToolParam(description = "Comma-separated index names or wildcards to filter. Omit for all indices.", required = false) String indices
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(metrics, indices))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
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

- [ ] **Step 3: Write ClusterStateToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClusterStateToolTest {

    private MockRestServiceServer mockServer;
    private ClusterStateTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new ClusterStateTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getClusterState_noMetrics_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/state"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"cluster_name\":\"test\"}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterState("local", null, null, null);

        mockServer.verify();
        assertThat(result).contains("cluster_name");
    }

    @Test
    void getClusterState_withMetrics_callsMetricsPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/state/nodes"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterState("local", null, "nodes", null);

        mockServer.verify();
        assertThat(result).contains("nodes");
    }

    @Test
    void getClusterState_withMetricsAndIndices_callsFullPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/state/metadata/my-index"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"metadata\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterState("local", null, "metadata", "my-index");

        mockServer.verify();
        assertThat(result).contains("metadata");
    }

    @Test
    void getClusterState_unknownCluster_returnsError() {
        String result = tool.getClusterState("unknown", null, null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getClusterState_bothNull_returnsError() {
        String result = tool.getClusterState(null, null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ClusterStateToolTest"
```

Expected: BUILD SUCCESSFUL, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/ClusterStateTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/ClusterStateToolTest.java
git commit -m "feat: add clusterUrl support to ClusterStateTool"
```

---

### Task 6: Update GetShardsTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetShardsTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetShardsToolTest.java`

- [ ] **Step 1: Replace GetShardsTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetShardsTool {

    private final ClusterResolver clusterResolver;

    public GetShardsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
    public String getShards(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to filter shards. Omit for all shards.", required = false) String index
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(index) + "?v=true&format=json")
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/shards";
        }
        return "/_cat/shards/" + index;
    }
}
```

- [ ] **Step 2: Replace GetShardsToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GetShardsToolTest {

    private MockRestServiceServer mockServer;
    private GetShardsTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetShardsTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getShards_noIndex_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/shards?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getShards("local", null, null);

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getShards_withIndex_callsIndexPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/shards/my-index?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getShards("local", null, "my-index");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getShards_unknownCluster_returnsError() {
        String result = tool.getShards("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getShards_bothNull_returnsError() {
        String result = tool.getShards(null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.GetShardsToolTest"
```

Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetShardsTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetShardsToolTest.java
git commit -m "feat: add clusterUrl support to GetShardsTool"
```

---

### Task 7: Update GetSegmentsTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetSegmentsTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetSegmentsToolTest.java`

- [ ] **Step 1: Replace GetSegmentsTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetSegmentsTool {

    private final ClusterResolver clusterResolver;

    public GetSegmentsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about Lucene segments in OpenSearch indices, including memory usage, document counts, segment sizes, and whether segments are committed or searchable.")
    public String getSegments(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to filter segments. Omit for all indices.", required = false) String index
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(index) + "?v=true&format=json")
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/segments";
        }
        return "/_cat/segments/" + index;
    }
}
```

- [ ] **Step 2: Replace GetSegmentsToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GetSegmentsToolTest {

    private MockRestServiceServer mockServer;
    private GetSegmentsTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetSegmentsTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getSegments_noIndex_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/segments?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getSegments("local", null, null);

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getSegments_withIndex_callsIndexPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/segments/my-index?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getSegments("local", null, "my-index");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getSegments_unknownCluster_returnsError() {
        String result = tool.getSegments("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getSegments_bothNull_returnsError() {
        String result = tool.getSegments(null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.GetSegmentsToolTest"
```

Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetSegmentsTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetSegmentsToolTest.java
git commit -m "feat: add clusterUrl support to GetSegmentsTool"
```

---

### Task 8: Update GetNodesTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesToolTest.java`

- [ ] **Step 1: Replace GetNodesTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetNodesTool {

    private final ClusterResolver clusterResolver;

    public GetNodesTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets detailed information about nodes in an OpenSearch cluster, including static information like host system details, JVM info, processor type, node settings, thread pools, and installed plugins. Metrics can be filtered to categories like: settings, os, process, jvm, thread_pool, transport, http, plugins, ingest.")
    public String getNodes(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId,
            @ToolParam(description = "Comma-separated metrics categories to retrieve (e.g. settings, os, process, jvm, thread_pool, transport, http, plugins, ingest). Omit for all metrics.", required = false) String metrics
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(nodeId, metrics))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
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

- [ ] **Step 2: Replace GetNodesToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GetNodesToolTest {

    private MockRestServiceServer mockServer;
    private GetNodesTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetNodesTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getNodes_noFilters_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, null, null);

        mockServer.verify();
        assertThat(result).contains("nodes");
    }

    @Test
    void getNodes_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, "node1", null);

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodes_withMetrics_callsMetricsPath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/jvm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, null, "jvm");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodes_withNodeIdAndMetrics_callsFullPath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1/jvm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, "node1", "jvm");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodes_unknownCluster_returnsError() {
        String result = tool.getNodes("unknown", null, null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getNodes_bothNull_returnsError() {
        String result = tool.getNodes(null, null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.GetNodesToolTest"
```

Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesToolTest.java
git commit -m "feat: add clusterUrl support to GetNodesTool"
```

---

### Task 9: Update GetNodesHotThreadsTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesHotThreadsTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesHotThreadsToolTest.java`

- [ ] **Step 1: Replace GetNodesHotThreadsTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetNodesHotThreadsTool {

    private final ClusterResolver clusterResolver;

    public GetNodesHotThreadsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
    public String getNodesHotThreads(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(nodeId))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_nodes/hot_threads";
        }
        return "/_nodes/" + nodeId + "/hot_threads";
    }
}
```

- [ ] **Step 2: Replace GetNodesHotThreadsToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GetNodesHotThreadsToolTest {

    private MockRestServiceServer mockServer;
    private GetNodesHotThreadsTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetNodesHotThreadsTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getNodesHotThreads_noFilter_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/hot_threads"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("::: thread info :::", MediaType.TEXT_PLAIN));

        String result = tool.getNodesHotThreads("local", null, null);

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodesHotThreads_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1/hot_threads"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("::: thread info :::", MediaType.TEXT_PLAIN));

        String result = tool.getNodesHotThreads("local", null, "node1");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodesHotThreads_unknownCluster_returnsError() {
        String result = tool.getNodesHotThreads("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getNodesHotThreads_bothNull_returnsError() {
        String result = tool.getNodesHotThreads(null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.GetNodesHotThreadsToolTest"
```

Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetNodesHotThreadsTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetNodesHotThreadsToolTest.java
git commit -m "feat: add clusterUrl support to GetNodesHotThreadsTool"
```

---

### Task 10: Update GetAllocationTool

**Files:**
- Modify: `opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetAllocationTool.java`
- Modify: `opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetAllocationToolTest.java`

- [ ] **Step 1: Replace GetAllocationTool**

```java
package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetAllocationTool {

    private final ClusterResolver clusterResolver;

    public GetAllocationTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
    public String getAllocation(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Node ID or name to filter allocation info for a specific node. Omit for all nodes.", required = false) String nodeId
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(nodeId) + "?v=true&format=json")
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_cat/allocation";
        }
        return "/_cat/allocation/" + nodeId;
    }
}
```

- [ ] **Step 2: Replace GetAllocationToolTest**

```java
package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GetAllocationToolTest {

    private MockRestServiceServer mockServer;
    private GetAllocationTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetAllocationTool(new ClusterResolver(Map.of("local", client)));
    }

    @Test
    void getAllocation_noFilter_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/allocation?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getAllocation("local", null, null);

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getAllocation_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/allocation/node1?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = tool.getAllocation("local", null, "node1");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getAllocation_unknownCluster_returnsError() {
        String result = tool.getAllocation("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void getAllocation_bothNull_returnsError() {
        String result = tool.getAllocation(null, null, null);
        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }
}
```

- [ ] **Step 3: Run all core tests**

```bash
./gradlew :opensearch-mcp-core:test
```

Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 4: Commit**

```bash
git add opensearch-mcp-core/src/main/java/org/o8h/mcp/core/tool/GetAllocationTool.java \
        opensearch-mcp-core/src/test/java/org/o8h/mcp/core/tool/GetAllocationToolTest.java
git commit -m "feat: add clusterUrl support to GetAllocationTool"
```

---

### Task 11: Update integration test for ad-hoc mode

**Files:**
- Modify: `opensearch-mcp-http/src/test/java/org/o8h/mcp/http/tool/ToolsIntegrationTest.java`

The existing tests call tools with `clusterName="local"`. Update each call to pass the new `clusterUrl=null` argument, and add one ad-hoc mode test that sets `RequestContextHolder` with real credentials and calls with `clusterUrl`.

- [ ] **Step 1: Update all existing test method calls to include the new null clusterUrl argument, and add the ad-hoc test**

Replace the entire file with:

```java
package org.o8h.mcp.http.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.tool.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ToolsIntegrationTest {

    @Autowired
    private GetShardsTool getShardsTool;

    @Autowired
    private ClusterHealthTool clusterHealthTool;

    @Autowired
    private GetSegmentsTool getSegmentsTool;

    @Autowired
    private GetNodesTool getNodesTool;

    @Autowired
    private GetNodesHotThreadsTool getNodesHotThreadsTool;

    @Autowired
    private GetAllocationTool getAllocationTool;

    @Value("${opensearch.clusters.local.url}")
    private String localClusterUrl;

    @Value("${opensearch.clusters.local.username:admin}")
    private String localUsername;

    @Value("${opensearch.clusters.local.password:}")
    private String localPassword;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getShards_allShards_returnsValidResponse() {
        String result = getShardsTool.getShards("local", null, null);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getShards_withWildcard_returnsValidResponse() {
        String result = getShardsTool.getShards("local", null, "*");
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void clusterHealth_overall_returnsStatus() {
        String result = clusterHealthTool.getClusterHealth("local", null, null);
        assertThat(result).isNotNull();
        assertThat(result).contains("status");
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void clusterHealth_withWildcard_returnsStatus() {
        String result = clusterHealthTool.getClusterHealth("local", null, "*");
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getSegments_allSegments_returnsValidResponse() {
        String result = getSegmentsTool.getSegments("local", null, null);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getNodes_allNodes_returnsNodeInfo() {
        String result = getNodesTool.getNodes("local", null, null, null);
        assertThat(result).isNotNull();
        assertThat(result).contains("nodes");
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getNodes_jvmMetrics_returnsJvmInfo() {
        String result = getNodesTool.getNodes("local", null, null, "jvm");
        assertThat(result).isNotNull();
        assertThat(result).contains("jvm");
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getNodesHotThreads_allNodes_returnsResponse() {
        String result = getNodesHotThreadsTool.getNodesHotThreads("local", null, null);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void getAllocation_allNodes_returnsAllocationInfo() {
        String result = getAllocationTool.getAllocation("local", null, null);
        assertThat(result).isNotNull();
        assertThat(result).doesNotContain("Unknown cluster");
    }

    @Test
    void clusterHealth_adHocMode_returnsStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", localUsername);
        request.addHeader("X-OpenSearch-Password", localPassword);
        request.addHeader("X-OpenSearch-SSL-Disabled", "true");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String result = clusterHealthTool.getClusterHealth(null, localClusterUrl, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("status");
        assertThat(result).doesNotContain("clusterName or clusterUrl");
    }
}
```

- [ ] **Step 2: Run integration tests** (requires local OpenSearch at `https://localhost:9200`)

```bash
./gradlew :opensearch-mcp-http:test
```

Expected: BUILD SUCCESSFUL, all tests green including `clusterHealth_adHocMode_returnsStatus`.

- [ ] **Step 3: Commit**

```bash
git add opensearch-mcp-http/src/test/java/org/o8h/mcp/http/tool/ToolsIntegrationTest.java
git commit -m "test: add ad-hoc mode integration test and update existing call signatures"
```

---

### Task 12: Full build verification

- [ ] **Step 1: Build both jars**

```bash
./gradlew :opensearch-mcp-http:bootJar :opensearch-mcp-stdio:bootJar
```

Expected: BUILD SUCCESSFUL. Jars at `build/libs/opensearch-mcp-http.jar` and `build/libs/opensearch-mcp-stdio.jar`.

- [ ] **Step 2: Run all tests**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests green across all modules.
