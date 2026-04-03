package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GenericOpenSearchApiToolTest {

    private MockRestServiceServer mockServer;
    private GenericOpenSearchApiTool toolWritesEnabled;
    private GenericOpenSearchApiTool toolWritesDisabled;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        ClusterResolver resolver = new ClusterResolver(Map.of("local", client));
        toolWritesEnabled = new GenericOpenSearchApiTool(resolver, true);
        toolWritesDisabled = new GenericOpenSearchApiTool(resolver, false);
    }

    @Test
    void callApi_get_withQueryParamsAndHeaders_dispatches() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/shards?v=true"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("X-Custom", "header-value"))
                .andRespond(withSuccess("[{\"shard\":\"0\"}]", MediaType.APPLICATION_JSON));

        String result = toolWritesEnabled.callApi(
                "local", null, "/_cat/shards", "GET",
                Map.of("v", "true"), null, Map.of("X-Custom", "header-value"));

        mockServer.verify();
        assertThat(result).contains("shard");
    }

    @Test
    void callApi_post_withBody_whenWritesEnabled_succeeds() {
        mockServer.expect(requestTo("http://localhost:9200/_search"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string("{\"query\":{\"match_all\":{}}}"))
                .andRespond(withSuccess("{\"hits\":{}}", MediaType.APPLICATION_JSON));

        String result = toolWritesEnabled.callApi(
                "local", null, "/_search", "POST",
                null, "{\"query\":{\"match_all\":{}}}", null);

        mockServer.verify();
        assertThat(result).contains("hits");
    }

    @Test
    void callApi_post_whenWritesDisabled_returnsError() {
        String result = toolWritesDisabled.callApi(
                "local", null, "/_doc", "POST",
                null, "{\"field\":\"value\"}", null);

        assertThat(result).contains("Write operations are disabled");
        assertThat(result).contains("opensearch.write-enabled=true");
    }

    @Test
    void callApi_delete_whenWritesDisabled_returnsError() {
        String result = toolWritesDisabled.callApi(
                "local", null, "/my-index/_doc/1", "DELETE",
                null, null, null);

        assertThat(result).contains("Write operations are disabled");
    }

    @Test
    void callApi_put_whenWritesDisabled_returnsError() {
        String result = toolWritesDisabled.callApi(
                "local", null, "/my-index", "PUT",
                null, "{\"settings\":{}}", null);

        assertThat(result).contains("Write operations are disabled");
    }

    @Test
    void callApi_unknownCluster_returnsError() {
        String result = toolWritesEnabled.callApi(
                "unknown", null, "/_search", "GET",
                null, null, null);

        assertThat(result).contains("Unknown cluster: unknown");
    }

    @Test
    void callApi_opensearchErrorResponse_returnsBodyAsString() {
        String errorBody = "{\"error\":{\"type\":\"index_not_found_exception\",\"reason\":\"no such index [logs]\"},\"status\":404}";
        mockServer.expect(requestTo("http://localhost:9200/logs/_search"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        String result = toolWritesEnabled.callApi(
                "local", null, "/logs/_search", "GET",
                null, null, null);

        assertThat(result).contains("index_not_found_exception");
    }

    @Test
    void callApi_bothNull_returnsError() {
        String result = toolWritesEnabled.callApi(
                null, null, "/_search", "GET",
                null, null, null);

        assertThat(result).contains("Either clusterName or clusterUrl must be provided");
    }

    @Test
    void callApi_invalidMethod_returnsError() {
        String result = toolWritesEnabled.callApi(
                "local", null, "/_search", "INVALID",
                null, null, null);

        assertThat(result).contains("Invalid method: INVALID");
    }
}
