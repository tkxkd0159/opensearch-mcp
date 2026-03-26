package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ClusterStateTool {

    private final Map<String, RestClient> clients;

    public ClusterStateTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets the current state of an OpenSearch cluster including node information, index metadata, shard routing, and blocks. Metrics can be filtered to: nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid. Indices can be filtered by name or wildcard.")
    public String getClusterState(
            @ToolParam(description = "Name of the target OpenSearch cluster.") String clusterName,
            @ToolParam(description = "Comma-separated metrics to retrieve (nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid). Omit for all metrics.", required = false) String metrics,
            @ToolParam(description = "Comma-separated index names or wildcards to filter. Omit for all indices.", required = false) String indices
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(metrics, indices))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String metrics, String indices) {
        if (metrics == null || metrics.isBlank()) {
            return "/_cluster/state";
        }
        if (indices == null || indices.isBlank()) {
            return "/_cluster/state/" + metrics;
        }
        return "/_cluster/state/" + metrics + "/" + indices;
    }
}
