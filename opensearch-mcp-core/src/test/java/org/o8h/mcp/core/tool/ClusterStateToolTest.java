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
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/state"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"cluster_name\":\"test\"}", MediaType.APPLICATION_JSON));

    String result = tool.getClusterState("local", null, null, null);

    mockServer.verify();
    assertThat(result).contains("cluster_name");
  }

  @Test
  void getClusterState_withMetrics_callsMetricsPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/state/nodes"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"nodes\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getClusterState("local", null, "nodes", null);

    mockServer.verify();
    assertThat(result).contains("nodes");
  }

  @Test
  void getClusterState_withMetricsAndIndices_callsFullPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/state/metadata/my-index"))
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
