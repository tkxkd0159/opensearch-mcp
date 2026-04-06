package org.o8h.mcp.http.tool;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.ClusterStateTool;
import org.o8h.mcp.core.tool.GenericOpenSearchApiTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
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
  private final GetNodesTool getNodesTool;
  private final GetNodesHotThreadsTool getNodesHotThreadsTool;
  private final GetAllocationTool getAllocationTool;
  private final GenericOpenSearchApiTool genericOpenSearchApiTool;

  /** Creates a new callbacks adapter for the HTTP transport. */
  public HttpToolCallbacks(
      HttpClusterTargetFactory clusterTargetFactory,
      ClusterHealthTool clusterHealthTool,
      ClusterStateTool clusterStateTool,
      GetShardsTool getShardsTool,
      GetSegmentsTool getSegmentsTool,
      GetNodesTool getNodesTool,
      GetNodesHotThreadsTool getNodesHotThreadsTool,
      GetAllocationTool getAllocationTool,
      GenericOpenSearchApiTool genericOpenSearchApiTool) {
    this.clusterTargetFactory = clusterTargetFactory;
    this.clusterHealthTool = clusterHealthTool;
    this.clusterStateTool = clusterStateTool;
    this.getShardsTool = getShardsTool;
    this.getSegmentsTool = getSegmentsTool;
    this.getNodesTool = getNodesTool;
    this.getNodesHotThreadsTool = getNodesHotThreadsTool;
    this.getAllocationTool = getAllocationTool;
    this.genericOpenSearchApiTool = genericOpenSearchApiTool;
  }

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
    return clusterHealthTool.getClusterHealth(clusterTargetFactory.create(clusterName, clusterUrl), index);
  }

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
    return getAllocationTool.getAllocation(clusterTargetFactory.create(clusterName, clusterUrl), nodeId);
  }

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
        clusterTargetFactory.create(clusterName, clusterUrl), path, method, queryParams, body, headers);
  }
}
