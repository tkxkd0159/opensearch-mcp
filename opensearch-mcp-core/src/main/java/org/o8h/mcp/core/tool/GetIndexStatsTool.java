package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;

/** Reads index statistics from OpenSearch. */
public class GetIndexStatsTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetIndexStatsTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches index stats using the native OpenSearch path shapes.
   *
   * @param target transport-neutral cluster target
   * @param indexIds optional index names, aliases, or wildcards
   * @param metrics optional stats metric selector
   * @return the raw JSON response or an error message
   */
  public String getIndexStats(
      @Nullable ClusterTarget target, @Nullable String indexIds, @Nullable String metrics) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(indexIds, metrics))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String indexIds, @Nullable String metrics) {
    boolean hasIndexIds = indexIds != null && !indexIds.isBlank();
    boolean hasMetrics = metrics != null && !metrics.isBlank();

    if (!hasIndexIds && !hasMetrics) {
      return "/_stats";
    }
    if (!hasIndexIds) {
      return "/_stats/" + metrics;
    }
    if (!hasMetrics) {
      return "/" + indexIds + "/_stats";
    }
    return "/" + indexIds + "/_stats/" + metrics;
  }
}
