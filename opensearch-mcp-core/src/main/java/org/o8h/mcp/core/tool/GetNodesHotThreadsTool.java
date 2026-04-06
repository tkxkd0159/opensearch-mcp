package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;

/** Reads hot-thread diagnostics from OpenSearch nodes. */
public class GetNodesHotThreadsTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetNodesHotThreadsTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches hot-thread output for all nodes or a selected set of nodes.
   *
   * @param target transport-neutral cluster target
   * @param nodeId optional node identifier filter
   * @return the raw text response or an error message
   */
  public String getNodesHotThreads(@Nullable ClusterTarget target, @Nullable String nodeId) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(nodeId))
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String nodeId) {
    if (nodeId == null || nodeId.isBlank()) {
      return "/_nodes/hot_threads";
    }
    return "/_nodes/" + nodeId + "/hot_threads";
  }
}
