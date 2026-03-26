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

class GetAllocationToolTest {

    private MockRestServiceServer mockServer;
    private GetAllocationTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetAllocationTool(Map.of("local", client));
    }

    @Test
    void getAllocation_noNodeId_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/allocation?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"node\":\"node1\",\"shards\":\"5\"}]", MediaType.APPLICATION_JSON));

        String result = tool.getAllocation("local", null);

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getAllocation_withNodeId_callsNodePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/allocation/node1?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"node\":\"node1\",\"shards\":\"5\"}]", MediaType.APPLICATION_JSON));

        String result = tool.getAllocation("local", "node1");

        mockServer.verify();
        assertThat(result).contains("node1");
    }

    @Test
    void getAllocation_unknownCluster_returnsError() {
        String result = tool.getAllocation("unknown", null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }
}
