package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.opensearch.ClusterResolver;

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
   * @param target transport-neutral cluster target
   * @param index optional index or wildcard filter
   * @return the raw JSON response or an error message
   */
  public String getClusterHealth(@Nullable ClusterTarget target, @Nullable String index) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
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
