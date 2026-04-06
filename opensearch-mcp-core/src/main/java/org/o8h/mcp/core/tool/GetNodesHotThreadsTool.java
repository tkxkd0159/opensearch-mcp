package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Reads hot-thread diagnostics from OpenSearch nodes. */
public class GetNodesHotThreadsTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetNodesHotThreadsTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches hot-thread output for all nodes or a selected set of nodes.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param nodeId optional node identifier filter
   * @return the raw text response or an error message
   */
  @Tool(
      description =
          "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
  public String getNodesHotThreads(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description = "Comma-separated node IDs or names to filter. Omit for all nodes.",
              required = false)
          @Nullable String nodeId) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
                .get()
                .uri(buildPath(nodeId))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String nodeId) {
    if (nodeId == null || nodeId.isBlank()) {
      return "/_nodes/hot_threads";
    }
    return "/_nodes/" + nodeId + "/hot_threads";
  }
}
