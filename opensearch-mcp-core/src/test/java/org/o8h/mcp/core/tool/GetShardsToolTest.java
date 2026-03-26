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

class GetShardsToolTest {

    private MockRestServiceServer mockServer;
    private GetShardsTool tool;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        tool = new GetShardsTool(Map.of("local", client));
    }

    @Test
    void getShards_noIndex_callsBasePath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/shards?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"index\":\"test\"}]", MediaType.APPLICATION_JSON));

        String result = tool.getShards("local", null);

        mockServer.verify();
        assertThat(result).contains("test");
    }

    @Test
    void getShards_withIndex_callsIndexPath() {
        mockServer.expect(requestTo("http://localhost:9200/_cat/shards/my-index?v=true&format=json"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"index\":\"my-index\"}]", MediaType.APPLICATION_JSON));

        String result = tool.getShards("local", "my-index");

        mockServer.verify();
        assertThat(result).contains("my-index");
    }

    @Test
    void getShards_unknownCluster_returnsError() {
        String result = tool.getShards("unknown", null);
        assertThat(result).contains("Unknown cluster: unknown");
        assertThat(result).contains("local");
    }
}
