package org.o8h.mcp.core.config;

import org.o8h.mcp.core.tool.ClusterStateTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider clusterStateTools(ClusterStateTool clusterStateTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(clusterStateTool)
                .build();
    }
}
