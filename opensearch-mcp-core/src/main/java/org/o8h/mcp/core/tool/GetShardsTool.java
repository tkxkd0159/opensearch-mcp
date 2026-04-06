package org.o8h.mcp.core.tool;

import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

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
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param index optional index filter
   * @return the raw JSON response or an error message
   */
  @Tool(
      description =
          "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
  public String getShards(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description = "Index name or wildcard pattern to filter shards. Omit for all shards.",
              required = false)
          @Nullable String index) {
    return ToolCallHelper.execute(
        () ->
            clusterResolver
                .resolve(clusterName, clusterUrl)
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
