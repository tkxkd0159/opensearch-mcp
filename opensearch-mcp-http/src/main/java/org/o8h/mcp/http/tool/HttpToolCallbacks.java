package org.o8h.mcp.http.tool;

import java.util.Map;
import org.jspecify.annotations.Nullable;
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
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/** HTTP-facing MCP tool callbacks that adapt request data to the core services. */
public class HttpToolCallbacks {

  private final HttpClusterTargetFactory clusterTargetFactory;
  private final ClusterHealthTool clusterHealthTool;
  private final ClusterStateTool clusterStateTool;
  private final GetShardsTool getShardsTool;
  private final GetSegmentsTool getSegmentsTool;
  private final CatNodesTool catNodesTool;
  private final GetNodesTool getNodesTool;
  private final GetIndexInfoTool getIndexInfoTool;
  private final GetIndexStatsTool getIndexStatsTool;
  private final GetNodesHotThreadsTool getNodesHotThreadsTool;
  private final GetAllocationTool getAllocationTool;
  private final GetLongRunningTasksTool getLongRunningTasksTool;
  private final GenericOpenSearchApiTool genericOpenSearchApiTool;

  /**
   * Creates a new callbacks adapter for the HTTP transport.
   *
   * @param clusterTargetFactory factory that resolves registered and ad-hoc cluster targets
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
   */
  public HttpToolCallbacks(
      HttpClusterTargetFactory clusterTargetFactory,
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
    this.clusterTargetFactory = clusterTargetFactory;
    this.clusterHealthTool = clusterHealthTool;
    this.clusterStateTool = clusterStateTool;
    this.getShardsTool = getShardsTool;
    this.getSegmentsTool = getSegmentsTool;
    this.catNodesTool = catNodesTool;
    this.getNodesTool = getNodesTool;
    this.getIndexInfoTool = getIndexInfoTool;
    this.getIndexStatsTool = getIndexStatsTool;
    this.getNodesHotThreadsTool = getNodesHotThreadsTool;
    this.getAllocationTool = getAllocationTool;
    this.getLongRunningTasksTool = getLongRunningTasksTool;
    this.genericOpenSearchApiTool = genericOpenSearchApiTool;
  }

  /**
   * Returns cluster health information for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param index optional index selector
   * @return cluster health response payload
   */
  @Tool(
      description =
          "Returns basic information about the health of an OpenSearch cluster, including status (green/yellow/red), number of nodes, active shards, and relocating/unassigned shards.")
  public String getClusterHealth(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Index name or wildcard pattern to get health for specific indices. Omit for cluster-wide health.",
              required = false)
          @Nullable String index) {
    return clusterHealthTool.getClusterHealth(
        clusterTargetFactory.create(clusterName, clusterUrl), index);
  }

  /**
   * Returns cluster state information for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param metrics optional comma-separated state metrics selector
   * @param indices optional comma-separated index selector
   * @return cluster state response payload
   */
  @Tool(
      description =
          "Gets the current state of an OpenSearch cluster including node information, index metadata, shard routing, and blocks. Metrics can be filtered to: nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid. Indices can be filtered by name or wildcard.")
  public String getClusterState(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Comma-separated metrics to retrieve (nodes, metadata, blocks, routing_table, routing_nodes, version, state_uuid). Omit for all metrics.",
              required = false)
          @Nullable String metrics,
      @ToolParam(
              description =
                  "Comma-separated index names or wildcards to filter. Omit for all indices.",
              required = false)
          @Nullable String indices) {
    return clusterStateTool.getClusterState(
        clusterTargetFactory.create(clusterName, clusterUrl), metrics, indices);
  }

  /**
   * Returns shard information for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param index optional index selector
   * @return shard response payload
   */
  @Tool(
      description =
          "Gets information about shards in an OpenSearch cluster, including shard state, size, node assignment, and primary/replica status.")
  public String getShards(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description = "Index name or wildcard pattern to filter shards. Omit for all shards.",
              required = false)
          @Nullable String index) {
    return getShardsTool.getShards(clusterTargetFactory.create(clusterName, clusterUrl), index);
  }

  /**
   * Returns Lucene segment information for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param index optional index selector
   * @return segment response payload
   */
  @Tool(
      description =
          "Gets information about Lucene segments in OpenSearch indices, including memory usage, document counts, segment sizes, and whether segments are committed or searchable.")
  public String getSegments(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Index name or wildcard pattern to filter segments. Omit for all indices.",
              required = false)
          @Nullable String index) {
    return getSegmentsTool.getSegments(clusterTargetFactory.create(clusterName, clusterUrl), index);
  }

  /**
   * Returns CAT node information for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param columns optional CAT column selector
   * @return CAT nodes response payload
   */
  @Tool(
      description =
          "Lists node-level CAT information in an OpenSearch cluster, including node roles and load metrics. Columns can be limited to a comma-separated list of CAT node column names.")
  public String catNodes(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Comma-separated CAT node columns to include via the _cat/nodes h parameter, such as ip,name or heap.percent,ram.percent.",
              required = false)
          @Nullable String columns) {
    return catNodesTool.catNodes(clusterTargetFactory.create(clusterName, clusterUrl), columns);
  }

  /**
   * Returns node details for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param nodeId optional node selector
   * @param metrics optional node metrics selector
   * @return node response payload
   */
  @Tool(
      description =
          "Gets detailed information about nodes in an OpenSearch cluster, including static information like host system details, JVM info, processor type, node settings, thread pools, and installed plugins. Metrics can be filtered to categories like: settings, os, process, jvm, thread_pool, transport, http, plugins, ingest.")
  public String getNodes(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description = "Comma-separated node IDs or names to filter. Omit for all nodes.",
              required = false)
          @Nullable String nodeId,
      @ToolParam(
              description =
                  "Comma-separated metrics categories to retrieve (e.g. settings, os, process, jvm, thread_pool, transport, http, plugins, ingest). Omit for all metrics.",
              required = false)
          @Nullable String metrics) {
    return getNodesTool.getNodes(
        clusterTargetFactory.create(clusterName, clusterUrl), nodeId, metrics);
  }

  /**
   * Returns metadata for the requested index selector.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param index required index selector
   * @return index metadata response payload
   */
  @Tool(
      description =
          "Gets detailed information about an index including mappings, settings, and aliases. The index selector may be a concrete index name, alias, or wildcard pattern.")
  public String getIndexInfo(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Required index name, alias, or wildcard pattern to retrieve metadata for.")
          String index) {
    return getIndexInfoTool.getIndexInfo(
        clusterTargetFactory.create(clusterName, clusterUrl), index);
  }

  /**
   * Returns statistics for the requested index selector.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param index optional index selector
   * @param metrics optional stats metrics selector
   * @return index stats response payload
   */
  @Tool(
      description =
          "Gets index statistics including document counts, store size, and indexing or search metrics. Index selectors are index names, aliases, or wildcard patterns.")
  public String getIndexStats(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Optional comma-separated index names, aliases, or wildcard patterns. Omit for cluster-wide stats.",
              required = false)
          @Nullable String index,
      @ToolParam(
              description =
                  "Optional comma-separated stats metric names, such as docs,store or indexing,search.",
              required = false)
          @Nullable String metrics) {
    return getIndexStatsTool.getIndexStats(
        clusterTargetFactory.create(clusterName, clusterUrl), index, metrics);
  }

  /**
   * Returns hot threads information for selected nodes.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param nodeId optional node selector
   * @return hot threads response payload
   */
  @Tool(
      description =
          "Gets information about hot threads on nodes in an OpenSearch cluster. Returns a breakdown of the hot threads on each selected node, useful for diagnosing performance issues.")
  public String getNodesHotThreads(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description = "Comma-separated node IDs or names to filter. Omit for all nodes.",
              required = false)
          @Nullable String nodeId) {
    return getNodesHotThreadsTool.getNodesHotThreads(
        clusterTargetFactory.create(clusterName, clusterUrl), nodeId);
  }

  /**
   * Returns shard allocation information for selected nodes.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param nodeId optional node selector
   * @return allocation response payload
   */
  @Tool(
      description =
          "Gets information about shard allocation across nodes in an OpenSearch cluster, including disk usage, shard counts per node, and available disk space.")
  public String getAllocation(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Node ID or name to filter allocation info for a specific node. Omit for all nodes.",
              required = false)
          @Nullable String nodeId) {
    return getAllocationTool.getAllocation(
        clusterTargetFactory.create(clusterName, clusterUrl), nodeId);
  }

  /**
   * Returns currently running tasks for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param minRunningSeconds optional minimum running time filter in seconds
   * @return tasks response payload
   */
  @Tool(
      description =
          "Gets currently running tasks in an OpenSearch cluster, sorted by running time descending. Optionally filters the result to tasks running at least the requested number of seconds.")
  public String getLongRunningTasks(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(
              description =
                  "Optional non-negative minimum running time in seconds. When provided, only tasks at or above this threshold are returned.",
              required = false)
          @Nullable Integer minRunningSeconds) {
    validateMinRunningSeconds(minRunningSeconds);
    return getLongRunningTasksTool.getLongRunningTasks(
        clusterTargetFactory.create(clusterName, clusterUrl), minRunningSeconds);
  }

  /**
   * Calls an arbitrary OpenSearch API for a registered or ad-hoc cluster target.
   *
   * @param clusterName registered cluster name, or {@code null} when using {@code clusterUrl}
   * @param clusterUrl ad-hoc cluster URL, or {@code null} when using {@code clusterName}
   * @param path API path that starts with {@code /}
   * @param method optional HTTP method
   * @param queryParams optional query string parameters
   * @param body optional raw JSON request body
   * @param headers optional additional HTTP headers
   * @return API response payload
   */
  @Tool(
      description =
          """
            Flexible interface to call any OpenSearch API endpoint. Use when:
            - Calling endpoints not covered by dedicated tools
            - Performing complex operations with custom parameters
            - Accessing newer OpenSearch features
            Write operations (POST, PUT, DELETE, PATCH) require opensearch.write-enabled=true.
            """)
  public String callApi(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(description = "API path, e.g. /_search or /my-index/_doc/1. Must start with /.")
          String path,
      @ToolParam(
              description =
                  "HTTP method: GET, POST, PUT, DELETE, PATCH, or HEAD. GET and HEAD are always allowed. POST, PUT, DELETE, PATCH require write operations to be enabled.")
          @Nullable String method,
      @ToolParam(
              description =
                  "Query string parameters to append to the URL, e.g. {\"v\": \"true\", \"format\": \"json\"}.",
              required = false)
          @Nullable Map<String, String> queryParams,
      @ToolParam(
              description =
                  "Raw JSON request body. Sent when non-null and non-blank. Valid for all methods including GET (e.g. GET /_search with a query DSL body).",
              required = false)
          @Nullable String body,
      @ToolParam(
              description = "Additional HTTP headers to include in the request.",
              required = false)
          @Nullable Map<String, String> headers) {
    return genericOpenSearchApiTool.callApi(
        clusterTargetFactory.create(clusterName, clusterUrl),
        path,
        method,
        queryParams,
        body,
        headers);
  }

  private void validateMinRunningSeconds(@Nullable Integer minRunningSeconds) {
    if (minRunningSeconds != null && minRunningSeconds < 0) {
      throw new IllegalArgumentException("minRunningSeconds must be non-negative.");
    }
  }
}
