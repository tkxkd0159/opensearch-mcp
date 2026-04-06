# OpenSearch MCP

OpenSearch MCP server for cluster inspection, diagnostics, and direct API access over MCP.

## Quick Start

- [USER_GUIDE.md](USER_GUIDE.md) for setup, client configuration, and example workflows
- [DEVELOPMENT.md](DEVELOPMENT.md) for build, test, and contributor workflows

## Supported MCP Tools

Most tools accept exactly one of these connection inputs:

- `clusterName`: a registered cluster name returned by `listClusters`
- `clusterUrl`: an ad-hoc OpenSearch URL for HTTP transport only. When using `clusterUrl`, send `X-OpenSearch-Authorization: <scheme> <credentials>` on the MCP request.

If both `clusterName` and `clusterUrl` are provided, the request is rejected.
If `clusterName` is used, `X-OpenSearch-Authorization` is ignored.

| Tool                 | Purpose                                                                         | Key Parameters                                                                                               |
| -------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `listClusters`       | Lists registered OpenSearch clusters with their name and URL.                   | None                                                                                                         |
| `getClusterHealth`   | Returns cluster or index health, including status, node count, and shard state. | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `getClusterState`    | Returns cluster state, including nodes, metadata, routing, and blocks.          | `clusterName` or `clusterUrl`, optional `metrics`, optional `indices`                                        |
| `getShards`          | Returns shard allocation and state using the `_cat/shards` API.                 | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `getSegments`        | Returns Lucene segment information using the `_cat/segments` API.               | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `catNodes`           | Returns `_cat/nodes` output as JSON, optionally limited to selected CAT columns.| `clusterName` or `clusterUrl`, optional `columns`                                                            |
| `getNodes`           | Returns node information, optionally filtered by node and metric categories.    | `clusterName` or `clusterUrl`, optional `nodeId`, optional `metrics`                                         |
| `getNodesHotThreads` | Returns hot thread output for all nodes or selected nodes.                      | `clusterName` or `clusterUrl`, optional `nodeId`                                                             |
| `getAllocation`      | Returns shard allocation and disk usage using the `_cat/allocation` API.        | `clusterName` or `clusterUrl`, optional `nodeId`                                                             |
| `getIndexInfo`       | Returns index metadata including mappings, settings, and aliases.               | `clusterName` or `clusterUrl`, required `index`                                                              |
| `getIndexStats`      | Returns index statistics using the native `_stats` path shapes.                 | `clusterName` or `clusterUrl`, optional `indexIds`, optional `metrics`                                       |
| `getLongRunningTasks`| Returns running CAT tasks sorted by running time descending, with optional threshold filtering. | `clusterName` or `clusterUrl`, optional `minRunningSeconds`                                    |
| `callApi`            | Calls any OpenSearch API path not covered by dedicated tools.                   | `clusterName` or `clusterUrl`, `path`, `method`, optional `queryParams`, optional `body`, optional `headers` |

`callApi` supports `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, and `HEAD`. Write methods (`POST`, `PUT`, `DELETE`, `PATCH`) require `opensearch.write-enabled=true`.
