package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;

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
   * @param target transport-neutral cluster target
   * @param metrics optional metric filter
   * @param indices optional index filter
   * @return the raw JSON response or an error message
   */
  public String getClusterState(
      @Nullable ClusterTarget target, @Nullable String metrics, @Nullable String indices) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
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
