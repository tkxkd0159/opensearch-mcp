package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GetShardsTool {

    private final Map<String, RestClient> clients;

    public GetShardsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
    public String getShards(
            @ToolParam(description = "Name of the target OpenSearch cluster.") String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to filter shards. Omit for all shards.", required = false) String index
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(index) + "?v=true&format=json")
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/shards";
        }
        return "/_cat/shards/" + index;
    }
}
