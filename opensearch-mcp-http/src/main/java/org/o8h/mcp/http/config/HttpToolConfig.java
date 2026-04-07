package org.o8h.mcp.http.config;

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
import org.o8h.mcp.http.tool.HttpClusterTargetFactory;
import org.o8h.mcp.http.tool.HttpToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the HTTP transport-specific MCP callback beans. */
@Configuration
public class HttpToolConfig {

  /** Creates a new configuration instance. */
  public HttpToolConfig() {}

  /**
   * Creates the HTTP cluster target factory bean.
   *
   * @return the HTTP cluster target factory
   */
  @Bean
  public HttpClusterTargetFactory httpClusterTargetFactory() {
    return new HttpClusterTargetFactory();
  }

  /**
   * Creates the HTTP transport callback adapter bean.
   *
   * @param httpClusterTargetFactory factory that derives cluster targets from HTTP requests
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
   * @return the HTTP transport callback adapter
   */
  @Bean
  public HttpToolCallbacks httpToolCallbacks(
      HttpClusterTargetFactory httpClusterTargetFactory,
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
    return new HttpToolCallbacks(
        httpClusterTargetFactory,
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
   * Creates the HTTP tool callback provider bean.
   *
   * @param httpToolCallbacks HTTP transport tool callbacks
   * @param listClustersTool registered cluster listing tool
   * @return the HTTP tool callback provider
   */
  @Bean
  public ToolCallbackProvider toolCallbackProvider(
      HttpToolCallbacks httpToolCallbacks, ListClustersTool listClustersTool) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(httpToolCallbacks, listClustersTool)
        .build();
  }
}
