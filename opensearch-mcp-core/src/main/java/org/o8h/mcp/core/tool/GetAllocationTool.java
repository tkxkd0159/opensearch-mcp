package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GetAllocationTool {

    private final Map<String, RestClient> clients;

    public GetAllocationTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
    public String getAllocation(
            @ToolParam(description = "Name of the target OpenSearch cluster.") String clusterName,
            @ToolParam(description = "Node ID or name to filter allocation info for a specific node. Omit for all nodes.", required = false) String nodeId
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId) + "?v=true&format=json")
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_cat/allocation";
        }
        return "/_cat/allocation/" + nodeId;
    }
}
