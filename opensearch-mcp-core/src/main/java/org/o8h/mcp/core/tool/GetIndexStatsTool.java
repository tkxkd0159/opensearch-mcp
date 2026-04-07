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
   * @param index optional index names, aliases, or wildcards
   * @param metrics optional stats metric selector
   * @return the raw JSON response or an error message
   */
  public String getIndexStats(
      @Nullable ClusterTarget target, @Nullable String index, @Nullable String metrics) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(index, metrics))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String index, @Nullable String metrics) {
    boolean hasIndex = index != null && !index.isBlank();
    boolean hasMetrics = metrics != null && !metrics.isBlank();

    if (!hasIndex && !hasMetrics) {
      return "/_stats";
    }
    if (!hasIndex) {
      return "/_stats/" + metrics;
    }
    if (!hasMetrics) {
      return "/" + index + "/_stats";
    }
    return "/" + index + "/_stats/" + metrics;
  }
}
