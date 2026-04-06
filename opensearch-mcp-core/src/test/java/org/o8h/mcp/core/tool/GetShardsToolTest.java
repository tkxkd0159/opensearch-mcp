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

class GetShardsToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

  private MockRestServiceServer mockServer;
  private GetShardsTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new GetShardsTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getShards_noIndex_callsBasePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/shards?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getShards(LOCAL, null);

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getShards_withIndex_callsIndexPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/shards/my-index?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getShards(LOCAL, "my-index");

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getShards_unknownCluster_returnsError() {
    String result = tool.getShards(UNKNOWN, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getShards_nullTarget_returnsError() {
    String result = tool.getShards(null, null);
    assertThat(result).contains("Cluster target is required.");
  }
}
