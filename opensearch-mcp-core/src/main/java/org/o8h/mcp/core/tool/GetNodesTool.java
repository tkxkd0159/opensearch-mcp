package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.opensearch.ClusterResolver;

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
   * @param target transport-neutral cluster target
   * @param nodeId optional node identifier filter
   * @param metrics optional metric filter
   * @return the raw JSON response or an error message
   */
  public String getNodes(
      @Nullable ClusterTarget target, @Nullable String nodeId, @Nullable String metrics) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
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
