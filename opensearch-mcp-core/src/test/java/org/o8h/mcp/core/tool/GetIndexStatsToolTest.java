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

class GetIndexStatsToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

  private MockRestServiceServer mockServer;
  private GetIndexStatsTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new GetIndexStatsTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getIndexStats_withoutFilters_callsBasePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_stats"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"_all\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexStats(LOCAL, null, null);

    mockServer.verify();
    assertThat(result).contains("_all");
  }

  @Test
  void getIndexStats_withMetrics_callsMetricsPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_stats/docs,store"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"_all\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexStats(LOCAL, null, "docs,store");

    mockServer.verify();
    assertThat(result).contains("_all");
  }

  @Test
  void getIndexStats_withIndexIds_callsIndexPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/books,logs/_stats"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"indices\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexStats(LOCAL, "books,logs", null);

    mockServer.verify();
    assertThat(result).contains("indices");
  }

  @Test
  void getIndexStats_withIndexIdsAndMetrics_callsCombinedPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/books,logs/_stats/docs,store"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"indices\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexStats(LOCAL, "books,logs", "docs,store");

    mockServer.verify();
    assertThat(result).contains("indices");
  }

  @Test
  void getIndexStats_unknownCluster_returnsError() {
    String result = tool.getIndexStats(UNKNOWN, null, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getIndexStats_nullTarget_returnsError() {
    String result = tool.getIndexStats(null, null, null);
    assertThat(result).contains("Cluster target is required.");
  }
}
