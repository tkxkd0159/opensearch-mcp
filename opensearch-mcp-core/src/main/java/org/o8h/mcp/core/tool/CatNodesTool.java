package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;

/** Reads CAT node information from OpenSearch. */
public class CatNodesTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public CatNodesTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches CAT node rows with optional column filtering.
   *
   * @param target transport-neutral cluster target
   * @param columns optional comma-separated CAT columns
   * @return the raw JSON response or an error message
   */
  public String catNodes(@Nullable ClusterTarget target, @Nullable String columns) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(columns))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String columns) {
    if (columns == null || columns.isBlank()) {
      return "/_cat/nodes?v=true&format=json";
    }
    return "/_cat/nodes?v=true&format=json&h=" + columns;
  }
}
