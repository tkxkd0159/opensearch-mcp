## Quick Start

```sh
# :<subproject-directory-name>:<task-name>

# Run tests
./gradlew :opensearch-mcp-core:test

# Run the application
./gradlew :opensearch-mcp-core:bootRun
./gradlew clean :opensearch-mcp-core:bootJar && java -jar build/libs/opensearch-mcp-core-0.0.1-SNAPSHOT.jar
curl http://localhost:8081/actuator 

# List all tasks
./gradlew :opensearch-mcp-core:tasks
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

```sh
# Initialize a session (Terminal 1)
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-03-26",
      "capabilities": {},
      "clientInfo": { "name": "test-client", "version": "1.0" }
    }
  }'

# Upgrade to Streaming (Terminal 2)
curl -N -X GET https://your-mcp-server.com/mcp \
  -H "Accept: text/event-stream" \
  -H "Mcp-Session-Id: sess_abc123"
    
# List available tools (Terminal 1)
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```