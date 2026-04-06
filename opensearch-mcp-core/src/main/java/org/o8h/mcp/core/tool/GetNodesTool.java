package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** Reads node information from OpenSearch. */
public class GetNodesTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetNodesTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches node details for all nodes or a selected subset.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param nodeId optional node identifier filter
   * @param metrics optional metric filter
   * @return the raw JSON response or an error message
   */
  @Tool(
      description =
          "Gets detailed information about nodes in an OpenSearch cluster, including static information like host system details, JVM info, processor type, node settings, thread pools, and installed plugins. Metrics can be filtered to categories like: settings, os, process, jvm, thread_pool, transport, http, plugins, ingest.")
  public String getNodes(
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
              description = "Comma-separated node IDs or names to filter. Omit for all nodes.",
              required = false)
          @Nullable String nodeId,
      @ToolParam(
              description =
                  "Comma-separated metrics categories to retrieve (e.g. settings, os, process, jvm, thread_pool, transport, http, plugins, ingest). Omit for all metrics.",
              required = false)
          @Nullable String metrics) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
                .get()
                .uri(buildPath(nodeId, metrics))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String nodeId, @Nullable String metrics) {
    boolean hasNodeId = nodeId != null && !nodeId.isBlank();
    boolean hasMetrics = metrics != null && !metrics.isBlank();

    if (!hasNodeId && !hasMetrics) return "/_nodes";
    if (hasNodeId && !hasMetrics) return "/_nodes/" + nodeId;
    if (!hasNodeId) return "/_nodes/" + metrics;
    return "/_nodes/" + nodeId + "/" + metrics;
  }
}
