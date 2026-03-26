package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GetNodesHotThreadsTool {

    private final Map<String, RestClient> clients;

    public GetNodesHotThreadsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
    public String getNodesHotThreads(
            @ToolParam(description = "Name of the target OpenSearch cluster.") String clusterName,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "/_nodes/hot_threads";
        }
        return "/_nodes/" + nodeId + "/hot_threads";
    }
}
