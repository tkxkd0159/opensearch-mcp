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

class CatNodesToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

  private MockRestServiceServer mockServer;
  private CatNodesTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new CatNodesTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void catNodes_withoutColumns_callsBasePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/nodes?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.catNodes(LOCAL, null);

    mockServer.verify();
    assertThat(result).isEqualTo("[]");
  }

  @Test
  void catNodes_withColumns_callsColumnFilteredPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/nodes?v=true&format=json&h=ip,name"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.catNodes(LOCAL, "ip,name");

    mockServer.verify();
    assertThat(result).isEqualTo("[]");
  }

  @Test
  void catNodes_unknownCluster_returnsError() {
    String result = tool.catNodes(UNKNOWN, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void catNodes_nullTarget_returnsError() {
    String result = tool.catNodes(null, null);
    assertThat(result).contains("Cluster target is required.");
  }
}
