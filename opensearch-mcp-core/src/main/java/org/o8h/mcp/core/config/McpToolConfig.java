package org.o8h.mcp.core.config;

import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.o8h.mcp.core.tool.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class McpToolConfig {

    @Bean
    public ClusterHealthTool clusterHealthTool(Map<String, RestClient> openSearchClients) {
        return new ClusterHealthTool(openSearchClients);
    }

    @Bean
    public ClusterStateTool clusterStateTool(Map<String, RestClient> openSearchClients) {
        return new ClusterStateTool(openSearchClients);
    }

    @Bean
    public GetShardsTool getShardsTool(Map<String, RestClient> openSearchClients) {
        return new GetShardsTool(openSearchClients);
    }

    @Bean
    public GetSegmentsTool getSegmentsTool(Map<String, RestClient> openSearchClients) {
        return new GetSegmentsTool(openSearchClients);
    }

    @Bean
    public GetNodesTool getNodesTool(Map<String, RestClient> openSearchClients) {
        return new GetNodesTool(openSearchClients);
    }

    @Bean
    public GetNodesHotThreadsTool getNodesHotThreadsTool(Map<String, RestClient> openSearchClients) {
        return new GetNodesHotThreadsTool(openSearchClients);
    }

    @Bean
    public GetAllocationTool getAllocationTool(Map<String, RestClient> openSearchClients) {
        return new GetAllocationTool(openSearchClients);
    }

    @Bean
    public ListClustersTool listClustersTool(OpenSearchProperties properties) {
        return new ListClustersTool(properties);
    }

    @Bean
    public ToolCallbackProvider allTools(
            ClusterHealthTool clusterHealthTool,
            ClusterStateTool clusterStateTool,
            GetShardsTool getShardsTool,
            GetSegmentsTool getSegmentsTool,
            GetNodesTool getNodesTool,
            GetNodesHotThreadsTool getNodesHotThreadsTool,
            GetAllocationTool getAllocationTool,
            ListClustersTool listClustersTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        clusterHealthTool, clusterStateTool, getShardsTool,
                        getSegmentsTool, getNodesTool, getNodesHotThreadsTool,
                        getAllocationTool, listClustersTool
                )
                .build();
    }
}
