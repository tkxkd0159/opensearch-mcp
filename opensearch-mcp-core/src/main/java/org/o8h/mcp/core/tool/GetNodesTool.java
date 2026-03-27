package org.o8h.mcp.core.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class GetNodesTool {

    private final Map<String, RestClient> clients;

    public GetNodesTool(Map<String, RestClient> clients) {
        this.clients = clients;
    }

    @Tool(description = "Gets detailed information about nodes in an OpenSearch cluster, including static information like host system details, JVM info, processor type, node settings, thread pools, and installed plugins. Metrics can be filtered to categories like: settings, os, process, jvm, thread_pool, transport, http, plugins, ingest.")
    public String getNodes(
            @ToolParam(description = "Name of the target OpenSearch cluster. Call listClusters to see available names.", required = true) String clusterName,
            @ToolParam(description = "Comma-separated node IDs or names to filter. Omit for all nodes.", required = false) String nodeId,
            @ToolParam(description = "Comma-separated metrics categories to retrieve (e.g. settings, os, process, jvm, thread_pool, transport, http, plugins, ingest). Omit for all metrics.", required = false) String metrics
    ) {
        RestClient client = clients.get(clusterName);
        if (client == null) {
            return "Unknown cluster: " + clusterName + ". Available clusters: " + clients.keySet();
        }
        return client.get()
                .uri(buildPath(nodeId, metrics))
                .retrieve()
                .body(String.class);
    }

    private String buildPath(String nodeId, String metrics) {
        boolean hasNodeId = nodeId != null && !nodeId.isBlank();
        boolean hasMetrics = metrics != null && !metrics.isBlank();

        if (!hasNodeId && !hasMetrics) return "/_nodes";
        if (hasNodeId && !hasMetrics) return "/_nodes/" + nodeId;
        if (!hasNodeId) return "/_nodes/" + metrics;
        return "/_nodes/" + nodeId + "/" + metrics;
    }
}
