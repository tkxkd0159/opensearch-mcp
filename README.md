## Quick Start

```sh
./gradlew :opensearch-mcp-http:runLocalJar # curl http://localhost:8081/actuator
./gradlew :opensearch-mcp-stdio:runLocalJar
```

## Development Checks

```sh
# build -> check -> test
./gradlew check                            # tests + Spotless + aggregate coverage + Javadoc
./gradlew spotlessApply                    # in-place formatting
./gradlew test                             # deterministic unit/default suite
./gradlew integrationTest
./gradlew jacocoAggregateReport           # generates the combined coverage report for inspection
./gradlew javadoc                         # aggregate root docs at build/docs/javadoc
```

`compose.yml` remains for manual smoke and local server workflows. Automated multi-node integration coverage now lives in `opensearch-mcp-core` and runs through Testcontainers.

Using MCP client:
```text
Add a document to 'logs' index. The document is {"message": "MCP test", "@timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"}

Return documents whose timestamp is within the last 120 minutes from the `logs` index on the `local` OpenSearch cluster
```

## Supported MCP Tools

Most tools accept exactly one of these connection inputs:

- `clusterName`: a registered cluster name returned by `listClusters`
- `clusterUrl`: an ad-hoc OpenSearch URL for HTTP transport only. When using `clusterUrl`, send `X-OpenSearch-Authorization: <scheme> <credentials>` on the MCP request.

If both clusterName and clusterUrl are provided, the request is rejected.
If clusterName is used, X-OpenSearch-Authorization is ignored.

| Tool                 | Purpose                                                                         | Key Parameters                                                                                               |
| -------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `listClusters`       | Lists registered OpenSearch clusters with their name and URL.                   | None                                                                                                         |
| `getClusterHealth`   | Returns cluster or index health, including status, node count, and shard state. | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `getClusterState`    | Returns cluster state, including nodes, metadata, routing, and blocks.          | `clusterName` or `clusterUrl`, optional `metrics`, optional `indices`                                        |
| `getShards`          | Returns shard allocation and state using the `_cat/shards` API.                 | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `getSegments`        | Returns Lucene segment information using the `_cat/segments` API.               | `clusterName` or `clusterUrl`, optional `index`                                                              |
| `getNodes`           | Returns node information, optionally filtered by node and metric categories.    | `clusterName` or `clusterUrl`, optional `nodeId`, optional `metrics`                                         |
| `getNodesHotThreads` | Returns hot thread output for all nodes or selected nodes.                      | `clusterName` or `clusterUrl`, optional `nodeId`                                                             |
| `getAllocation`      | Returns shard allocation and disk usage using the `_cat/allocation` API.        | `clusterName` or `clusterUrl`, optional `nodeId`                                                             |
| `callApi`            | Calls any OpenSearch API path not covered by dedicated tools.                   | `clusterName` or `clusterUrl`, `path`, `method`, optional `queryParams`, optional `body`, optional `headers` |

`callApi` supports `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, and `HEAD`. Write methods (`POST`, `PUT`, `DELETE`, `PATCH`) require `opensearch.write-enabled=true`.
