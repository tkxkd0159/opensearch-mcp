package org.o8h.mcp.core.config;

import java.util.Map;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.o8h.mcp.core.tool.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Registers the OpenSearch-backed MCP tools as Spring beans. */
@Configuration
public class McpToolConfig {

  /** Creates a new configuration instance. */
  public McpToolConfig() {}

  /**
   * Creates the resolver used by tools to find registered or ad-hoc clusters.
   *
   * @param openSearchClients preconfigured clients keyed by cluster name
   * @return the shared cluster resolver
   */
  @Bean
  public ClusterResolver clusterResolver(Map<String, RestClient> openSearchClients) {
    return new ClusterResolver(openSearchClients);
  }

  /**
   * Creates the cluster health tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the cluster health tool
   */
  @Bean
  public ClusterHealthTool clusterHealthTool(ClusterResolver clusterResolver) {
    return new ClusterHealthTool(clusterResolver);
  }

  /**
   * Creates the cluster state tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the cluster state tool
   */
  @Bean
  public ClusterStateTool clusterStateTool(ClusterResolver clusterResolver) {
    return new ClusterStateTool(clusterResolver);
  }

  /**
   * Creates the shard listing tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the shard tool
   */
  @Bean
  public GetShardsTool getShardsTool(ClusterResolver clusterResolver) {
    return new GetShardsTool(clusterResolver);
  }

  /**
   * Creates the segment inspection tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the segment tool
   */
  @Bean
  public GetSegmentsTool getSegmentsTool(ClusterResolver clusterResolver) {
    return new GetSegmentsTool(clusterResolver);
  }

  /**
   * Creates the node inspection tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the node tool
   */
  @Bean
  public GetNodesTool getNodesTool(ClusterResolver clusterResolver) {
    return new GetNodesTool(clusterResolver);
  }

  /**
   * Creates the hot-threads tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the hot-threads tool
   */
  @Bean
  public GetNodesHotThreadsTool getNodesHotThreadsTool(ClusterResolver clusterResolver) {
    return new GetNodesHotThreadsTool(clusterResolver);
  }

  /**
   * Creates the allocation tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @return the allocation tool
   */
  @Bean
  public GetAllocationTool getAllocationTool(ClusterResolver clusterResolver) {
    return new GetAllocationTool(clusterResolver);
  }

  /**
   * Creates the registered-cluster listing tool bean.
   *
   * @param properties OpenSearch configuration properties
   * @return the list-clusters tool
   */
  @Bean
  public ListClustersTool listClustersTool(OpenSearchProperties properties) {
    return new ListClustersTool(properties);
  }

  /**
   * Creates the generic OpenSearch API tool bean.
   *
   * @param clusterResolver cluster resolver shared by all tools
   * @param properties OpenSearch configuration properties
   * @return the generic OpenSearch API tool
   */
  @Bean
  public GenericOpenSearchApiTool genericOpenSearchApiTool(
      ClusterResolver clusterResolver, OpenSearchProperties properties) {
    return new GenericOpenSearchApiTool(clusterResolver, properties.isWriteEnabled());
  }

  /**
   * Exposes all MCP tools through Spring AI's callback provider.
   *
   * @param clusterHealthTool cluster health tool bean
   * @param clusterStateTool cluster state tool bean
   * @param getShardsTool shard tool bean
   * @param getSegmentsTool segment tool bean
   * @param getNodesTool node tool bean
   * @param getNodesHotThreadsTool hot-threads tool bean
   * @param getAllocationTool allocation tool bean
   * @param listClustersTool list-clusters tool bean
   * @param genericOpenSearchApiTool generic API tool bean
   * @return the tool callback provider used by the MCP server
   */
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
      GenericOpenSearchApiTool genericOpenSearchApiTool) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(
            clusterHealthTool,
            clusterStateTool,
            getShardsTool,
            getSegmentsTool,
            getNodesTool,
            getNodesHotThreadsTool,
            getAllocationTool,
            listClustersTool,
            genericOpenSearchApiTool)
        .build();
  }
}
