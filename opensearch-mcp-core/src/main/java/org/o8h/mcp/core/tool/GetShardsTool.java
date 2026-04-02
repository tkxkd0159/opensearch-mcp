package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetShardsTool {

    private final ClusterResolver clusterResolver;

    public GetShardsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
    public String getShards(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to filter shards. Omit for all shards.", required = false) String index
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(index) + "?v=true&format=json")
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/shards";
        }
        return "/_cat/shards/" + index;
    }
}
