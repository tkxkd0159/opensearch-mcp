package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class ClusterHealthTool {

    private final Map<String, RestClient> clients;

    public ClusterHealthTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
    public String getClusterHealth(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.", required = false) String index
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(index))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cluster/health";
        }
        return "/_cluster/health/" + index;
    }
}
