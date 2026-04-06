package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;

/** Reads detailed index metadata from OpenSearch. */
public class GetIndexInfoTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetIndexInfoTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches index metadata for the requested index selector.
   *
   * @param target transport-neutral cluster target
   * @param index required index name, alias, or wildcard
   * @return the raw JSON response or an error message
   */
  public String getIndexInfo(@Nullable ClusterTarget target, String index) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(index))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(String index) {
    if (index.isBlank()) {
      throw new IllegalArgumentException("index is required.");
    }
    return "/" + index;
  }
}
