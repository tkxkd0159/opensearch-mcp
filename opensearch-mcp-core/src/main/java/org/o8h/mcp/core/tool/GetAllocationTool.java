package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Reads node allocation information from OpenSearch. */
public class GetAllocationTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetAllocationTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches allocation details for all nodes or a selected node.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param nodeId optional node identifier filter
   * @return the raw JSON response or an error message
   */
  @Tool(
      description =
          "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
  public String getAllocation(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Node ID or name to filter allocation info for a specific node. Omit for all nodes.",
              required = false)
          @Nullable String nodeId) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
                .get()
                .uri(buildPath(nodeId) + "?v=true&format=json")
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String nodeId) {
    if (nodeId == null || nodeId.isBlank()) {
      return "/_cat/allocation";
    }
    return "/_cat/allocation/" + nodeId;
  }
}
