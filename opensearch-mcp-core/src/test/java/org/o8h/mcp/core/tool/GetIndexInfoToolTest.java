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

class GetIndexInfoToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

  private MockRestServiceServer mockServer;
  private GetIndexInfoTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new GetIndexInfoTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getIndexInfo_withConcreteIndex_callsIndexPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/books"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"books\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexInfo(LOCAL, "books");

    mockServer.verify();
    assertThat(result).contains("books");
  }

  @Test
  void getIndexInfo_withWildcard_callsWildcardPath() {
    mockServer
        .expect(requestTo("http://localhost:9200/logs-*"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"logs-2026\":{}}", MediaType.APPLICATION_JSON));

    String result = tool.getIndexInfo(LOCAL, "logs-*");

    mockServer.verify();
    assertThat(result).contains("logs-2026");
  }

  @Test
  void getIndexInfo_unknownCluster_returnsError() {
    String result = tool.getIndexInfo(UNKNOWN, "books");
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getIndexInfo_nullTarget_returnsError() {
    String result = tool.getIndexInfo(null, "books");
    assertThat(result).contains("Cluster target is required.");
  }
}
