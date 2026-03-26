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