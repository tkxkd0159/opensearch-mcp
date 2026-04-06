package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Reads cluster health information from OpenSearch. */
public class ClusterHealthTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public ClusterHealthTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches health information for a cluster or a specific index pattern.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param index optional index or wildcard filter
   * @return the raw JSON response or an error message
   */
  @Tool(
      description =
          "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
  public String getClusterHealth(
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
                  "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.",
              required = false)
          @Nullable String index) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
                .get()
                .uri(buildPath(index))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String index) {
    if (index == null || index.isBlank()) {
      return "/_cluster/health";
    }
    return "/_cluster/health/" + index;
  }
}
