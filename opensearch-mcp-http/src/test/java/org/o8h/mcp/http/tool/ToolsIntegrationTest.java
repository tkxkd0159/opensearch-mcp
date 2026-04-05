package org.o8h.mcp.http.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
import org.o8h.mcp.core.tool.GetNodesHotThreadsTool;
import org.o8h.mcp.core.tool.GetNodesTool;
import org.o8h.mcp.core.tool.GetSegmentsTool;
import org.o8h.mcp.core.tool.GetShardsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class ToolsIntegrationTest {

  @Autowired private GetShardsTool getShardsTool;

  @Autowired private ClusterHealthTool clusterHealthTool;

  @Autowired private GetSegmentsTool getSegmentsTool;

  @Autowired private GetNodesTool getNodesTool;

  @Autowired private GetNodesHotThreadsTool getNodesHotThreadsTool;

  @Autowired private GetAllocationTool getAllocationTool;

  @Test
  void getShards_allShards_returnsValidResponse() {
    String result = getShardsTool.getShards("local", null, null);
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getShards_withWildcard_returnsValidResponse() {
    String result = getShardsTool.getShards("local", null, "*");
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void clusterHealth_overall_returnsStatus() {
    String result = clusterHealthTool.getClusterHealth("local", null, null);
    assertThat(result).isNotNull();
    assertThat(result).contains("status");
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void clusterHealth_withWildcard_returnsStatus() {
    String result = clusterHealthTool.getClusterHealth("local", null, "*");
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getSegments_allSegments_returnsValidResponse() {
    String result = getSegmentsTool.getSegments("local", null, null);
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getNodes_allNodes_returnsNodeInfo() {
    String result = getNodesTool.getNodes("local", null, null, null);
    assertThat(result).isNotNull();
    assertThat(result).contains("nodes");
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getNodes_jvmMetrics_returnsJvmInfo() {
    String result = getNodesTool.getNodes("local", null, null, "jvm");
    assertThat(result).isNotNull();
    assertThat(result).contains("jvm");
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getNodesHotThreads_allNodes_returnsResponse() {
    String result = getNodesHotThreadsTool.getNodesHotThreads("local", null, null);
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }

  @Test
  void getAllocation_allNodes_returnsAllocationInfo() {
    String result = getAllocationTool.getAllocation("local", null, null);
    assertThat(result).isNotNull();
    assertThat(result).doesNotContain("Unknown cluster");
  }
}
