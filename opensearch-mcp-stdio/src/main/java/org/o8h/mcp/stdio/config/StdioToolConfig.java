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

  /**
   * Creates the stdio transport callback adapter bean.
   *
   * @param clusterHealthTool tool that retrieves cluster health information
   * @param clusterStateTool tool that retrieves cluster state information
   * @param getShardsTool tool that lists shard information
   * @param getSegmentsTool tool that retrieves segment information
   * @param catNodesTool tool that lists CAT node information
   * @param getNodesTool tool that retrieves node information
   * @param getIndexInfoTool tool that retrieves index metadata
   * @param getIndexStatsTool tool that retrieves index statistics
   * @param getNodesHotThreadsTool tool that retrieves node hot threads information
   * @param getAllocationTool tool that retrieves shard allocation information
   * @param getLongRunningTasksTool tool that retrieves long-running task information
   * @param genericOpenSearchApiTool tool that calls arbitrary OpenSearch APIs
   * @return the stdio transport callback adapter
   */
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

  /**
   * Creates the stdio tool callback provider bean.
   *
   * @param stdioToolCallbacks stdio transport tool callbacks
   * @param listClustersTool registered cluster listing tool
   * @return the stdio tool callback provider
   */
  @Bean
  public ToolCallbackProvider toolCallbackProvider(
      StdioToolCallbacks stdioToolCallbacks, ListClustersTool listClustersTool) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(stdioToolCallbacks, listClustersTool)
        .build();
  }
}
