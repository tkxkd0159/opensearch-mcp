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

class GetLongRunningTasksToolTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");
  private static final ClusterTarget.Registered UNKNOWN = new ClusterTarget.Registered("unknown");

  private MockRestServiceServer mockServer;
  private GetLongRunningTasksTool tool;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient client = builder.build();
    tool = new GetLongRunningTasksTool(new ClusterResolver(Map.of("local", client)));
  }

  @Test
  void getLongRunningTasks_withoutThreshold_callsDefaultPath() {
    mockServer
        .expect(
            requestTo("http://localhost:9200/_cat/tasks?v=true&format=json&s=running_time:desc"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    String result = tool.getLongRunningTasks(LOCAL, null);

    mockServer.verify();
    assertThat(result).isEqualTo("[]");
  }

  @Test
  void getLongRunningTasks_withThreshold_filtersRowsBelowMinimum() {
    mockServer
        .expect(
            requestTo(
                "http://localhost:9200/_cat/tasks?v=true&format=json&s=running_time:desc&time=s"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                [
                  {"action":"slow","running_time":"12.0s"},
                  {"action":"fast","running_time":"2.0s"}
                ]
                """,
                MediaType.APPLICATION_JSON));

    String result = tool.getLongRunningTasks(LOCAL, 5);

    mockServer.verify();
    assertThat(result).contains("slow").doesNotContain("fast");
  }

  @Test
  void getLongRunningTasks_unknownCluster_returnsError() {
    String result = tool.getLongRunningTasks(UNKNOWN, null);
    assertThat(result).contains("Unknown cluster: unknown");
  }

  @Test
  void getLongRunningTasks_nullTarget_returnsError() {
    String result = tool.getLongRunningTasks(null, null);
    assertThat(result).contains("Cluster target is required.");
  }
}
