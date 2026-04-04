## Quick Start

```sh
./gradlew :opensearch-mcp-http:runLocalJar # curl http://localhost:8081/actuator
./gradlew :opensearch-mcp-stdio:runLocalJar
```

## MCP (Streamable HTTP transport)

| Endpoint | Method | Purpose                                            |
|----------|--------|----------------------------------------------------|
| /mcp     | POST   | Send MCP requests (initialize, tool calls, etc.)   | 
| /mcp     | GET    | Open a stream to receive server-initiated messages |
| /mcp     | DELETE | Terminate the session                              |

1. Handshake (POST): The client sends an initialize request. The server creates a session and returns a unique Mcp-Session-Id.
2. Streaming Establishment (GET): The client "upgrades" the session by opening a long-lived GET request to the same endpoint. This creates the "Downstream" (Server $\rightarrow$ Client) pipe.
3. Interaction (POST): The client sends tool calls or resource requests via standard POSTs using the same Session ID.
4. Asynchronous Delivery: The server pushes the results or notifications back through the open GET stream.

---
<details>
    <summary>MCP Flow</summary>

```sh
# Initialize a session (Terminal 1)
curl -v -X POST http://localhost:8080/mcp \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream, application/json" \
    -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'

# (optional) Upgrade to Streaming (Terminal 2)
curl -N -X GET http://localhost:8080/mcp \
  -H "Accept: text/event-stream" \
  -H "Mcp-Session-Id: <returned-session-id>"
    
# List available tools (Terminal 1)
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream, application/json" \
  -H "Mcp-Session-Id: <returned-session-id>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

Using MCP client:
```text
Add a document to 'logs' index. The document is {"message": "MCP test", "@timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"}

Return documents whose timestamp is within the last 120 minutes from the `logs` index on the `local` OpenSearch cluster
```
</details>
