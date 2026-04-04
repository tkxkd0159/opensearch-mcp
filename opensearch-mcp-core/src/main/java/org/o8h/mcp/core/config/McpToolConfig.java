package org.o8h.mcp.core.config;

import org.o8h.mcp.core.opensearch.ClusterResolver;
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
    public ClusterResolver clusterResolver(Map<String, RestClient> openSearchClients) {
        return new ClusterResolver(openSearchClients);
    }

    @Bean
    public ClusterHealthTool clusterHealthTool(ClusterResolver clusterResolver) {
        return new ClusterHealthTool(clusterResolver);
    }

    @Bean
    public ClusterStateTool clusterStateTool(ClusterResolver clusterResolver) {
        return new ClusterStateTool(clusterResolver);
    }

    @Bean
    public GetShardsTool getShardsTool(ClusterResolver clusterResolver) {
        return new GetShardsTool(clusterResolver);
    }

    @Bean
    public GetSegmentsTool getSegmentsTool(ClusterResolver clusterResolver) {
        return new GetSegmentsTool(clusterResolver);
    }

    @Bean
    public GetNodesTool getNodesTool(ClusterResolver clusterResolver) {
        return new GetNodesTool(clusterResolver);
    }

    @Bean
    public GetNodesHotThreadsTool getNodesHotThreadsTool(ClusterResolver clusterResolver) {
        return new GetNodesHotThreadsTool(clusterResolver);
    }

    @Bean
    public GetAllocationTool getAllocationTool(ClusterResolver clusterResolver) {
        return new GetAllocationTool(clusterResolver);
    }

    @Bean
    public ListClustersTool listClustersTool(OpenSearchProperties properties) {
        return new ListClustersTool(properties);
    }

    @Bean
    public GenericOpenSearchApiTool genericOpenSearchApiTool(
            ClusterResolver clusterResolver, OpenSearchProperties properties) {
        return new GenericOpenSearchApiTool(clusterResolver, properties.isWriteEnabled());
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
            ListClustersTool listClustersTool,
            GenericOpenSearchApiTool genericOpenSearchApiTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        clusterHealthTool, clusterStateTool, getShardsTool,
                        getSegmentsTool, getNodesTool, getNodesHotThreadsTool,
                        getAllocationTool, listClustersTool, genericOpenSearchApiTool
                )
                .build();
    }
}
