package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetSegmentsTool {

    private final Map<String, RestClient> clients;

    public GetSegmentsTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets information about Lucene segments in OpenSearch indices, including memory usage, document counts, segment sizes, and whether segments are committed or searchable.")
    public String getSegments(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Index name or wildcard pattern to filter segments. Omit for all indices.", required = false) String index
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
            return "/_cat/segments";
        }
        return "/_cat/segments/" + index;
    }
}
