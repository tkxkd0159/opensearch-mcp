#!/usr/bin/env bash

source ./scripts/_get_session.sh

curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -H "Mcp-Session-Id: $MCP_SESSION_ID" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "getClusterState",
      "arguments": {
        "clusterName": "local"
      }
    }
  }'