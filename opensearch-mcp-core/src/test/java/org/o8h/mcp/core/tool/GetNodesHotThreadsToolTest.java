package org.o8h.mcp.core.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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
    mockServer
        .expect(requestTo("http://localhost:9200/_nodes/hot_threads"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("::: thread info :::", MediaType.TEXT_PLAIN));

    String result = tool.getNodesHotThreads("local", null, null);

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getNodesHotThreads_withNodeId_callsNodePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_nodes/node1/hot_threads"))
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
    assertThat(result).contains("Provide exactly one of clusterName or clusterUrl.");
  }
}
