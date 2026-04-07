# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
# Full build + test suite
./gradlew build

# All checks: tests, Spotless formatting, JaCoCo coverage, Javadoc
./gradlew check

# Run tests only
./gradlew test

# Module-scoped tests
./gradlew :opensearch-mcp-core:test
./gradlew :opensearch-mcp-http:test
./gradlew :opensearch-mcp-stdio:test

# Run a single test class
./gradlew :opensearch-mcp-core:test --tests "org.o8h.mcp.core.tool.ClusterHealthToolTest"

# Integration tests (require Docker / running OpenSearch)
./gradlew :opensearch-mcp-core:integrationTest
./gradlew :opensearch-mcp-http:integrationTest

# Apply formatter (run before committing)
./gradlew spotlessApply

# Build fat JARs (outputs to build/libs/)
./gradlew :opensearch-mcp-http:bootJar :opensearch-mcp-stdio:bootJar

# Run locally with the `local` Spring profile
./gradlew :opensearch-mcp-http:runLocalJar
./gradlew :opensearch-mcp-stdio:runLocalJar

# Start the local OpenSearch stack + HTTP server
docker compose up --build
```

## Module Architecture

Three Gradle modules:

- **opensearch-mcp-core** — shared library: all MCP tool implementations, `ClusterResolver`, `OpenSearchConfig`. No `main()`. Unit tests live here.
- **opensearch-mcp-http** — Spring Boot app (port 8080, actuator 8081) using Streamable HTTP transport. Activated via `@EnableOpensearchMcp`.
- **opensearch-mcp-stdio** — Spring Boot app (no web server) using stdio transport. Same `@EnableOpensearchMcp` activation.

Build conventions (Java 25, test settings, JaCoCo) live in `build-logic/src/main/kotlin/org.o8h.java-conventions.gradle.kts`.

## Key Wiring: Adding a New Tool

`@EnableOpensearchMcp` (meta-annotation in `opensearch-mcp-core`) imports `CoreToolConfig`. That config class is the single factory for all tool beans — Spring AI's MCP server picks up `ToolCallbackProvider` automatically. **To add a new tool:** create the tool class in `org.o8h.mcp.core.tool`, declare it as a `@Bean` in `CoreToolConfig`. No transport-layer changes needed.

## Dual-Mode Cluster Resolution

`ClusterResolver` resolves a `ClusterTarget` to a `RestClient`:

1. **Registered** (`ClusterTarget.Registered`) — cluster name mapped from `opensearch.clusters.<name>` in `application.yml`.
2. **Ad-hoc** (`ClusterTarget.AdHoc`) — HTTP transport only; URL passed directly by the tool call. Credentials come from `X-OpenSearch-Authorization` HTTP header; TLS relaxation from `X-OpenSearch-SSL-Disabled`. Tool calls must provide exactly one of `clusterName` or `clusterUrl`.

## Configuration Shape

```yaml
opensearch:
  write-enabled: false        # gates POST/PUT/DELETE/PATCH in GenericOpenSearchApiTool (callApi)
  clusters:
    my-cluster:
      url: https://localhost:9200
      username: admin
      password: ${OPENSEARCH_PASSWORD:}
      ssl-verification-disabled: true
```

Local overrides go in `application-local.yml` (git-ignored). Activate with `--spring.profiles.active=local`. Tests can also read a repo-root `_test.yml`.

## Testing Patterns

Unit tests use JUnit 5, AssertJ, and `MockRestServiceServer` to mock `RestClient` at the HTTP level — no Mockito mocking of `RestClient` itself. See `ClusterHealthToolTest` for the canonical pattern:

```java
RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:9200");
MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
RestClient client = builder.build();
MyTool tool = new MyTool(new ClusterResolver(Map.of("local", client)));
```

Name test methods descriptively: `getClusterHealth_unknownCluster_returnsError`. Integration tests use Testcontainers (see `ToolsIntegrationTest` and `OpenSearchClusterFixture`).

## Coding & Commit Conventions

- Java 25, package `org.o8h.mcp.*`, 4-space indent, PascalCase classes, lowerCamelCase methods/fields.
- Commit subjects use conventional prefixes: `feat:`, `fix:`, `refactor:`, `build:`, `docs:`, `chore:`.
- Develop each feature/bugfix in a fresh git worktree (`.worktrees/` is git-ignored).
- Use `.github/pull_request_template.md` and `gh pr create` when opening PRs.
- Reference `.mcp.example.json` for local MCP client setup; never commit real credentials.
