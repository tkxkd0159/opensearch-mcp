#!/usr/bin/env bash

INIT_RESPONSE=$(curl -si -X POST http://localhost:8080/mcp \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream, application/json" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}')

# shellcheck disable=SC2034
MCP_SESSION_ID=$(echo "$INIT_RESPONSE" | grep -i '^mcp-session-id:' | awk '{print $2}' | tr -d '\r')
