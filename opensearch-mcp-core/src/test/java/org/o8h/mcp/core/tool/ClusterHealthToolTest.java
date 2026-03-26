package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        tool = new ClusterHealthTool(Map.of("local", client));
    }

    @Test
    void getClusterHealth_noIndex_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"green\"}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterHealth("local", null);

        mockServer.verify();
        assertThat(result).contains("green");
    }

    @Test
    void getClusterHealth_withIndex_callsIndexPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cluster/health/my-index"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"yellow\",\"indices\":{\"my-index\":{}}}", MediaType.APPLICATION_JSON));

        String result = tool.getClusterHealth("local", "my-index");

        mockServer.verify();
        assertThat(result).contains("yellow");
    }

    @Test
    void getClusterHealth_unknownCluster_returnsError() {
        String result = tool.getClusterHealth("unknown", null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }
}
