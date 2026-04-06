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

class GetSegmentsToolTest {

  private MockRestServiceServer mockServer;
  private GetSegmentsTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new GetSegmentsTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getSegments_noIndex_callsBasePath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/segments?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getSegments("local", null, null);

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getSegments_withIndex_callsIndexPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/_cat/segments/my-index?v=true&format=json"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getSegments("local", null, "my-index");

    mockServer.verify();
    assertThat(result).isNotNull();
  }

  @Test
  void getSegments_unknownCluster_returnsError() {
    String result = tool.getSegments("unknown", null, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getSegments_bothNull_returnsError() {
    String result = tool.getSegments(null, null, null);
    assertThat(result).contains("Provide exactly one of clusterName or clusterUrl.");
  }
}
