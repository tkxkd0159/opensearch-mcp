package org.o8h.mcp.core.config;

import java.util.Map;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.OpenSearchProperties;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Registers transport-agnostic OpenSearch MCP services as Spring beans. */
@Configuration
public class CoreToolConfig {

  /** Creates a new configuration instance. */
  public CoreToolConfig() {}

  /**
   * Creates the resolver used by services to find registered or ad-hoc clusters.
   *
   * @param openSearchClients preconfigured clients keyed by cluster name
   * @return the shared cluster resolver
   */
  @Bean
  public ClusterResolver clusterResolver(Map<String, RestClient> openSearchClients) {
    return new ClusterResolver(openSearchClients);
  }

  /**
   * Creates the cluster health service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the cluster health service
   */
  @Bean
  public ClusterHealthTool clusterHealthTool(ClusterResolver clusterResolver) {
    return new ClusterHealthTool(clusterResolver);
  }

  /**
   * Creates the cluster state service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the cluster state service
   */
  @Bean
  public ClusterStateTool clusterStateTool(ClusterResolver clusterResolver) {
    return new ClusterStateTool(clusterResolver);
  }

  /**
   * Creates the shard listing service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the shard service
   */
  @Bean
  public GetShardsTool getShardsTool(ClusterResolver clusterResolver) {
    return new GetShardsTool(clusterResolver);
  }

  /**
   * Creates the segment inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the segment service
   */
  @Bean
  public GetSegmentsTool getSegmentsTool(ClusterResolver clusterResolver) {
    return new GetSegmentsTool(clusterResolver);
  }

  /**
   * Creates the CAT nodes inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the CAT nodes service
   */
  @Bean
  public CatNodesTool catNodesTool(ClusterResolver clusterResolver) {
    return new CatNodesTool(clusterResolver);
  }

  /**
   * Creates the node inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the node service
   */
  @Bean
  public GetNodesTool getNodesTool(ClusterResolver clusterResolver) {
    return new GetNodesTool(clusterResolver);
  }

  /**
   * Creates the index metadata inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the index metadata service
   */
  @Bean
  public GetIndexInfoTool getIndexInfoTool(ClusterResolver clusterResolver) {
    return new GetIndexInfoTool(clusterResolver);
  }

  /**
   * Creates the index stats inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the index stats service
   */
  @Bean
  public GetIndexStatsTool getIndexStatsTool(ClusterResolver clusterResolver) {
    return new GetIndexStatsTool(clusterResolver);
  }

  /**
   * Creates the hot-threads service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the hot-threads service
   */
  @Bean
  public GetNodesHotThreadsTool getNodesHotThreadsTool(ClusterResolver clusterResolver) {
    return new GetNodesHotThreadsTool(clusterResolver);
  }

  /**
   * Creates the allocation service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the allocation service
   */
  @Bean
  public GetAllocationTool getAllocationTool(ClusterResolver clusterResolver) {
    return new GetAllocationTool(clusterResolver);
  }

  /**
   * Creates the long-running tasks inspection service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @return the long-running tasks service
   */
  @Bean
  public GetLongRunningTasksTool getLongRunningTasksTool(ClusterResolver clusterResolver) {
    return new GetLongRunningTasksTool(clusterResolver);
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
   * Creates the generic OpenSearch API service bean.
   *
   * @param clusterResolver cluster resolver shared by all services
   * @param properties OpenSearch configuration properties
   * @return the generic OpenSearch API service
   */
  @Bean
  public GenericOpenSearchApiTool genericOpenSearchApiTool(
      ClusterResolver clusterResolver, OpenSearchProperties properties) {
    return new GenericOpenSearchApiTool(clusterResolver, properties.isWriteEnabled());
  }
}
