package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Reads cluster state information from OpenSearch. */
public class ClusterStateTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public ClusterStateTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches cluster state for the selected metrics and indices.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param metrics optional metric filter
   * @param indices optional index filter
   * @return the raw JSON response or an error message
   */
  @Tool(
      description =
          "Gets the current state of an OpenSearch cluster including node information, index metadata, shard routing, and blocks. Metrics can be filtered to: nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid. Indices can be filtered by name or wildcard.")
  public String getClusterState(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Comma-separated metrics to retrieve (nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid). Omit for all metrics.",
              required = false)
          @Nullable String metrics,
      @ToolParam(
              description =
                  "Comma-separated index names or wildcards to filter. Omit for all indices.",
              required = false)
          @Nullable String indices) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
                .get()
                .uri(buildPath(metrics, indices))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String metrics, @Nullable String indices) {
    if (metrics == null || metrics.isBlank()) {
      return "/_cluster/state";
    }
    if (indices == null || indices.isBlank()) {
      return "/_cluster/state/" + metrics;
    }
    return "/_cluster/state/" + metrics + "/" + indices;
  }
}
