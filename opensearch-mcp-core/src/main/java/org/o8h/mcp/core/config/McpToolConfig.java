package org.o8h.mcp.core.config;

import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.ClusterStateTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
import org.o8h.mcp.core.tool.GetNodesTool;
import org.o8h.mcp.core.tool.GetNodesHotThreadsTool;
import org.o8h.mcp.core.tool.GetSegmentsTool;
import org.o8h.mcp.core.tool.GetShardsTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider allTools(
            ClusterStateTool clusterStateTool,
            GetShardsTool getShardsTool,
            ClusterHealthTool clusterHealthTool,
            GetSegmentsTool getSegmentsTool,
            GetNodesTool getNodesTool,
            GetNodesHotThreadsTool getNodesHotThreadsTool,
            GetAllocationTool getAllocationTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        clusterStateTool,
                        getShardsTool,
                        clusterHealthTool,
                        getSegmentsTool,
                        getNodesTool,
                        getNodesHotThreadsTool,
                        getAllocationTool
                )
                .build();
    }
}
