package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.opensearch.ClusterResolver;

/** Reads node allocation information from OpenSearch. */
public class GetAllocationTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetAllocationTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches allocation details for all nodes or a selected node.
   *
   * @param target transport-neutral cluster target
   * @param nodeId optional node identifier filter
   * @return the raw JSON response or an error message
   */
  public String getAllocation(@Nullable ClusterTarget target, @Nullable String nodeId) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(nodeId) + "?v=true&format=json")
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String nodeId) {
    if (nodeId == null || nodeId.isBlank()) {
      return "/_cat/allocation";
    }
    return "/_cat/allocation/" + nodeId;
  }
}
