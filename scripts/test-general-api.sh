#!/usr/bin/env bash

source ./scripts/_get_session.sh
# Call GenericOpenSearchApiTool — index a document (requires opensearch.write-enabled=true)
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -H "Mcp-Session-Id: $MCP_SESSION_ID" \
  -d @- <<EOF
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "callApi",
    "arguments": {
      "clusterName": "local",
      "path": "/logs/_doc/1",
      "method": "PUT",
      "body": "{\"message\":\"error: disk usage exceeded threshold\",\"@timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"level\":\"ERROR\"}"
    }
  }
}
EOF

# Call GenericOpenSearchApiTool — search with a query DSL body and query params
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -H "Mcp-Session-Id: $MCP_SESSION_ID" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "callApi",
      "arguments": {
        "clusterName": "local",
        "path": "/_search",
        "method": "POST",
        "queryParams": {
          "size": "5"
        },
        "body": "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"message\":\"error\"}}],\"filter\":[{\"range\":{\"@timestamp\":{\"gte\":\"now-24h\"}}}]}}}"
      }
    }
  }'