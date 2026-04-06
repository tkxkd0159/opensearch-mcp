package org.o8h.mcp.stdio.config;

import org.o8h.mcp.core.tool.CatNodesTool;
import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.ClusterStateTool;
import org.o8h.mcp.core.tool.GenericOpenSearchApiTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
import org.o8h.mcp.core.tool.GetIndexInfoTool;
import org.o8h.mcp.core.tool.GetIndexStatsTool;
import org.o8h.mcp.core.tool.GetLongRunningTasksTool;
import org.o8h.mcp.core.tool.GetNodesHotThreadsTool;
import org.o8h.mcp.core.tool.GetNodesTool;
import org.o8h.mcp.core.tool.GetSegmentsTool;
import org.o8h.mcp.core.tool.GetShardsTool;
import org.o8h.mcp.core.tool.ListClustersTool;
import org.o8h.mcp.stdio.tool.StdioToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the stdio transport-specific MCP callback beans. */
@Configuration
public class StdioToolConfig {

  /** Creates a new configuration instance. */
  public StdioToolConfig() {}

  @Bean
  public StdioToolCallbacks stdioToolCallbacks(
      ClusterHealthTool clusterHealthTool,
      ClusterStateTool clusterStateTool,
      GetShardsTool getShardsTool,
      GetSegmentsTool getSegmentsTool,
      CatNodesTool catNodesTool,
      GetNodesTool getNodesTool,
      GetIndexInfoTool getIndexInfoTool,
      GetIndexStatsTool getIndexStatsTool,
      GetNodesHotThreadsTool getNodesHotThreadsTool,
      GetAllocationTool getAllocationTool,
      GetLongRunningTasksTool getLongRunningTasksTool,
      GenericOpenSearchApiTool genericOpenSearchApiTool) {
    return new StdioToolCallbacks(
        clusterHealthTool,
        clusterStateTool,
        getShardsTool,
        getSegmentsTool,
        catNodesTool,
        getNodesTool,
        getIndexInfoTool,
        getIndexStatsTool,
        getNodesHotThreadsTool,
        getAllocationTool,
        getLongRunningTasksTool,
        genericOpenSearchApiTool);
  }

  @Bean
  public ToolCallbackProvider toolCallbackProvider(
      StdioToolCallbacks stdioToolCallbacks, ListClustersTool listClustersTool) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(stdioToolCallbacks, listClustersTool)
        .build();
  }
}
