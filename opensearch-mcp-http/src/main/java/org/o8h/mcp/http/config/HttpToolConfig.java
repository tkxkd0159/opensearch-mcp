package org.o8h.mcp.http.config;

import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.ClusterStateTool;
import org.o8h.mcp.core.tool.GenericOpenSearchApiTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
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

  @Bean
  public HttpClusterTargetFactory httpClusterTargetFactory() {
    return new HttpClusterTargetFactory();
  }

  @Bean
  public HttpToolCallbacks httpToolCallbacks(
      HttpClusterTargetFactory httpClusterTargetFactory,
      ClusterHealthTool clusterHealthTool,
      ClusterStateTool clusterStateTool,
      GetShardsTool getShardsTool,
      GetSegmentsTool getSegmentsTool,
      GetNodesTool getNodesTool,
      GetNodesHotThreadsTool getNodesHotThreadsTool,
      GetAllocationTool getAllocationTool,
      GenericOpenSearchApiTool genericOpenSearchApiTool) {
    return new HttpToolCallbacks(
        httpClusterTargetFactory,
        clusterHealthTool,
        clusterStateTool,
        getShardsTool,
        getSegmentsTool,
        getNodesTool,
        getNodesHotThreadsTool,
        getAllocationTool,
        genericOpenSearchApiTool);
  }

  @Bean
  public ToolCallbackProvider toolCallbackProvider(
      HttpToolCallbacks httpToolCallbacks, ListClustersTool listClustersTool) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(httpToolCallbacks, listClustersTool)
        .build();
  }
}
