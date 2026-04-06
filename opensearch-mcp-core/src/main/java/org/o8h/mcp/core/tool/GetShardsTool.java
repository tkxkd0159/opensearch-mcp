package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.opensearch.ClusterResolver;

/** Reads shard information from OpenSearch. */
public class GetShardsTool {

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetShardsTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches shard details for all shards or a selected index pattern.
   *
   * @param target transport-neutral cluster target
   * @param index optional index filter
   * @return the raw JSON response or an error message
   */
  public String getShards(@Nullable ClusterTarget target, @Nullable String index) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(target)
                .get()
                .uri(buildPath(index) + "?v=true&format=json")
                .retrieve()
                .body(String.class));
  }

  private String buildPath(@Nullable String index) {
    if (index == null || index.isBlank()) {
      return "/_cat/shards";
    }
    return "/_cat/shards/" + index;
  }
}
