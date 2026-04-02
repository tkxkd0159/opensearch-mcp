package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetNodesHotThreadsTool {

    private final ClusterResolver clusterResolver;

    public GetNodesHotThreadsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
    public String getNodesHotThreads(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(nodeId))
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_nodes/hot_threads";
        }
        return "/_nodes/" + nodeId + "/hot_threads";
    }
}
