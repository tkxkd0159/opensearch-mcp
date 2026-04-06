package org.o8h.mcp.core.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GetAllocationToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

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
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/allocation?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getAllocation(LOCAL, null);

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getAllocation_withNodeId_callsNodePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/allocation/node1?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getAllocation(LOCAL, "node1");

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getAllocation_unknownCluster_returnsError() {
    String result = tool.getAllocation(UNKNOWN, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getAllocation_nullTarget_returnsError() {
    String result = tool.getAllocation(null, null);
    assertThat(result).contains("Cluster target is required.");
  }
}
