package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetAllocationTool {

    private final ClusterResolver clusterResolver;

    public GetAllocationTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
    public String getAllocation(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Node ID or name to filter allocation info for a specific node. Omit for all nodes.", required = false) String nodeId
    ) {
        try {
            return clusterResolver.resolve(clusterName, clusterUrl).get()
                    .uri(buildPath(nodeId) + "?v=true&format=json")
                    .retrieve()
                    .body(String.class);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_cat/allocation";
        }
        return "/_cat/allocation/" + nodeId;
    }
}
