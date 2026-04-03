package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class GetSegmentsTool {

    private final ClusterResolver clusterResolver;

    public GetSegmentsTool(ClusterResolver clusterResolver) {
        this.clusterResolver = clusterResolver;
    }

    @Tool(description = "Gets information about Lucene segments in OpenSearch indices, including memory usage, document counts, segment sizes, and whether segments are committed or searchable.")
    public String getSegments(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "Index name or wildcard pattern to filter segments. Omit for all indices.", required = false) String index
    ) {
        return ToolCallHelper.execute(() ->
                clusterResolver.resolve(clusterName, clusterUrl).get()
                        .uri(buildPath(index) + "?v=true&format=json")
                        .retrieve()
                        .body(String.class));
    }

    private String buildPath(String index) {
        if (index == null || index.isBlank()) {
            return "/_cat/segments";
        }
        return "/_cat/segments/" + index;
    }
}
