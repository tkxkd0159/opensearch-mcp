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

class GetNodesHotThreadsToolTest {

    private MockRestServiceServer mockServer;
    private GetNodesHotThreadsTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetNodesHotThreadsTool(Map.of("local", client));
    }

    @Test
    void getNodesHotThreads_noNodeId_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/hot_threads"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("::: {node1}", MediaType.TEXT_PLAIN));

        String result = tool.getNodesHotThreads("local", null);

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getNodesHotThreads_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_nodes/node1/hot_threads"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("::: {node1}", MediaType.TEXT_PLAIN));

        String result = tool.getNodesHotThreads("local", "node1");

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getNodesHotThreads_unknownCluster_returnsError() {
        String result = tool.getNodesHotThreads("unknown", null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }
}
