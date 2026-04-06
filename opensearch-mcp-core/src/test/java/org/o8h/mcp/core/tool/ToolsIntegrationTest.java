package org.o8h.mcp.core.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.config.CoreToolConfig;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.opensearch.OpenSearchConfig;
import org.o8h.mcp.core.tool.support.OpenSearchClusterFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Tag("integration")
@SpringJUnitConfig(classes = {OpenSearchConfig.class, CoreToolConfig.class})
class ToolsIntegrationTest {

  private static final ClusterTarget.Registered LOCAL = new ClusterTarget.Registered("local");

  @Autowired private ClusterHealthTool clusterHealthTool;
  @Autowired private ClusterStateTool clusterStateTool;
  @Autowired private CatNodesTool catNodesTool;
  @Autowired private GetNodesTool getNodesTool;
  @Autowired private GetIndexInfoTool getIndexInfoTool;
  @Autowired private GetIndexStatsTool getIndexStatsTool;
  @Autowired private GetAllocationTool getAllocationTool;
  @Autowired private GetShardsTool getShardsTool;
  @Autowired private GetSegmentsTool getSegmentsTool;
  @Autowired private GetNodesHotThreadsTool getNodesHotThreadsTool;
  @Autowired private GetLongRunningTasksTool getLongRunningTasksTool;
  @Autowired private GenericOpenSearchApiTool genericOpenSearchApiTool;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    OpenSearchClusterFixture.registerLocalClusterProperties(registry);
  }

  @AfterAll
  static void stopCluster() {
    OpenSearchClusterFixture.stop();
  }

  @Test
  void clusterHealth_reportsTwoNodes() {
    DocumentContext response = JsonPath.parse(clusterHealthTool.getClusterHealth(LOCAL, null));

    assertThat(response.<Integer>read("$.number_of_nodes")).isEqualTo(2);
    assertThat(response.<String>read("$.status")).isEqualTo("green");
    assertThat(response.<String>read("$.cluster_name")).isNotBlank();
  }

  @Test
  void clusterState_reportsBooksIndexAndTwoNodes() {
    DocumentContext response =
        JsonPath.parse(clusterStateTool.getClusterState(LOCAL, "metadata,nodes", "books"));

    assertThat(response.<Map<String, Object>>read("$.metadata.indices")).containsKey("books");
    assertThat(response.<Map<String, Object>>read("$.nodes")).hasSize(2);
  }

  @Test
  void nodesApi_returnsBothNodes() {
    DocumentContext response = JsonPath.parse(getNodesTool.getNodes(LOCAL, null, null));

    assertThat(response.<Map<String, Object>>read("$.nodes")).hasSize(2);
  }

  @Test
  void catNodes_returnsTwoNodeRows() {
    java.util.List<Map<String, Object>> response =
        readRows(catNodesTool.catNodes(LOCAL, "name,ip"));

    assertThat(response).hasSize(2);
    assertThat(response).allMatch(row -> row.containsKey("name") && row.containsKey("ip"));
  }

  @Test
  void indexInfo_returnsBooksMetadata() {
    DocumentContext response = JsonPath.parse(getIndexInfoTool.getIndexInfo(LOCAL, "books"));

    assertThat(response.<Map<String, Object>>read("$.books.settings")).isNotEmpty();
    assertThat(response.<Map<String, Object>>read("$.books.mappings")).isNotNull();
    assertThat(response.<Map<String, Object>>read("$.books.aliases")).isNotNull();
  }

  @Test
  void indexStats_returnsDocumentCountForBooks() {
    DocumentContext response =
        JsonPath.parse(getIndexStatsTool.getIndexStats(LOCAL, "books", "docs"));

    assertThat(response.<Integer>read("$.indices.books.primaries.docs.count")).isEqualTo(1);
  }

  @Test
  void allocationAndShards_placePrimaryAndReplicaOnDifferentNodes() {
    java.util.List<Map<String, Object>> shardRows =
        readRows(getShardsTool.getShards(LOCAL, "books"));

    Set<String> prirep =
        shardRows.stream().map(row -> value(row, "prirep")).collect(Collectors.toSet());
    Set<String> shardNodes =
        shardRows.stream()
            .map(row -> value(row, "node"))
            .filter(nodeName -> !nodeName.isBlank())
            .collect(Collectors.toSet());

    assertThat(prirep).contains("p", "r");
    assertThat(shardNodes).hasSize(2);

    java.util.List<Map<String, Object>> allocationRows =
        readRows(getAllocationTool.getAllocation(LOCAL, null));
    Set<String> allocationNodes =
        allocationRows.stream()
            .map(row -> value(row, "node"))
            .filter(nodeName -> !nodeName.isBlank())
            .collect(Collectors.toSet());

    assertThat(allocationNodes).containsAll(shardNodes);
  }

  @Test
  void segments_returnsEntriesForBooks() {
    java.util.List<Map<String, Object>> response =
        readRows(getSegmentsTool.getSegments(LOCAL, "books"));

    assertThat(response).isNotEmpty();
    assertThat(response.stream().map(row -> value(row, "index"))).anyMatch("books"::equals);
  }

  @Test
  void hotThreads_returnsNonEmptyResponse() {
    String response = getNodesHotThreadsTool.getNodesHotThreads(LOCAL, null);

    assertThat(response).isNotBlank();
    assertThat(response).contains(":::");
  }

  @Test
  void longRunningTasks_returnsJsonArray() {
    Object root = JsonPath.parse(getLongRunningTasksTool.getLongRunningTasks(LOCAL, null)).json();

    assertThat(root).isInstanceOf(java.util.List.class);
  }

  @Test
  void genericApi_getClusterHealth_succeeds() {
    String response =
        genericOpenSearchApiTool.callApi(
            LOCAL, "/_cluster/health", "GET", Map.of("pretty", "false"), null, null);

    DocumentContext json = JsonPath.parse(response);
    assertThat(json.<Integer>read("$.number_of_nodes")).isEqualTo(2);
    assertThat(json.<String>read("$.cluster_name")).isNotBlank();
  }

  private static String value(Map<String, Object> row, String key) {
    return Objects.toString(row.get(key), "");
  }

  @SuppressWarnings("unchecked")
  private static java.util.List<Map<String, Object>> readRows(String json) {
    Object root = JsonPath.parse(json).json();
    if (!(root instanceof java.util.List<?> rows)) {
      throw new AssertionError("Expected JSON array response but got: " + json);
    }
    return ((java.util.List<Object>) rows).stream().map(ToolsIntegrationTest::toMap).toList();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> toMap(Object row) {
    assertThat(row).as("expected JSON object row").isInstanceOf(Map.class);
    return (Map<String, Object>) row;
  }
}
