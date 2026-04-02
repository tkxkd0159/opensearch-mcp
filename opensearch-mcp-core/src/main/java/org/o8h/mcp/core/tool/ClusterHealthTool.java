package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ClusterHealthTool {

    private final ClusterResolver clusterResolver;

    public ClusterHealthTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
    public String getClusterHealth(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.", required = false) String index
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(index))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cluster/health";
        }
        return "/_cluster/health/" + index;
    }
}
