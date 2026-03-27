package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

public class ListClustersTool {

    private final OpenSearchProperties properties;

    public ListClustersTool(OpenSearchProperties properties) {
        this.properties = properties;
    }

    @Tool(description = "Lists all available OpenSearch clusters with their name and URL. Use the name as the clusterName parameter in other tools.")
    public List<Map<String, String>> listClusters() {
        return properties.getClusters().entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "url", e.getValue().getUrl()))
                .toList();
    }
}
