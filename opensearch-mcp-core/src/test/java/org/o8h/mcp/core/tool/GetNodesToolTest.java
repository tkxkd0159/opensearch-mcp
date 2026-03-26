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

class GetNodesToolTest {

    private MockRestServiceServer mockServer;
    private GetNodesTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetNodesTool(Map.of("local", client));
    }

    @Test
    void getNodes_noParams_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, null);

        mockServer.verify();
        assertThat(result).contains("nodes");
    }

    @Test
    void getNodes_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{\"node1\":{}}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", "node1", null);

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getNodes_withMetricsOnly_callsMetricsPath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/jvm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", null, "jvm");

        mockServer.verify();
        assertThat(result).isNotNull();
    }

    @Test
    void getNodes_withNodeIdAndMetrics_callsFullPath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1/jvm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nodes\":{\"node1\":{\"jvm\":{}}}}", MediaType.APPLICATION_JSON));

        String result = tool.getNodes("local", "node1", "jvm");

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getNodes_unknownCluster_returnsError() {
        String result = tool.getNodes("unknown", null, null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }
}
