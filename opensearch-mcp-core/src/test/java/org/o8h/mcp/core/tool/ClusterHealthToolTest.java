package org.o8h.mcp.core.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ClusterHealthToolTest {

  private MockRestServiceServer mockServer;
  private ClusterHealthTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new ClusterHealthTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getClusterHealth_noIndex_callsBasePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/health"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"status\":\"green\"}", MediaType.APPLICATION_JSON));

    String result = tool.getClusterHealth("local", null, null);

    mockServer.verify();
    assertThat(result).contains("green");
  }

  @Test
  void getClusterHealth_withIndex_callsIndexPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/health/my-index"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"status\":\"yellow\"}", MediaType.APPLICATION_JSON));

    String result = tool.getClusterHealth("local", null, "my-index");

    mockServer.verify();
    assertThat(result).contains("yellow");
  }

  @Test
  void getClusterHealth_unknownCluster_returnsError() {
    String result = tool.getClusterHealth("unknown", null, null);
    assertThat(result).contains("Unknown cluster: unknown");
    assertThat(result).contains("local");
  }

  @Test
  void getClusterHealth_bothNull_returnsError() {
    String result = tool.getClusterHealth(null, null, null);
    assertThat(result).contains("Either clusterName or clusterUrl must be provided");
  }

  @Test
  void getClusterHealth_httpErrorResponse_returnsResponseBody() {
    String errorBody = "{\"error\":{\"type\":\"index_not_found_exception\"},\"status\":404}";
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/health/missing-index"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody));

    String result = tool.getClusterHealth("local", null, "missing-index");

    mockServer.verify();
    assertThat(result).contains("index_not_found_exception");
  }

  @Test
  void getClusterHealth_networkFailure_returnsNetworkError() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cluster/health"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withException(new IOException("Connection refused")));

    String result = tool.getClusterHealth("local", null, null);

    mockServer.verify();
    assertThat(result).startsWith("Network error:");
    assertThat(result).contains("Connection refused");
  }
}
