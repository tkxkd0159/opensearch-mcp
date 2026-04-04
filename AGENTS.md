# Repository Guidelines

## Project Structure & Module Organization
`opensearch-mcp-core` contains shared OpenSearch tools, configuration, and most unit tests under `src/test/java`. `opensearch-mcp-http` exposes the streamable HTTP transport, and `opensearch-mcp-stdio` packages the stdio server. Shared Gradle conventions live in `build-logic/`. Use `proxy/` for the Node-based OIDC bridge, and `docker/`, `compose.yml`, and `scripts/` for local cluster and MCP smoke-test support.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repository root.

- `./gradlew build`: build all modules and run the full test suite.
- `./gradlew test`: run JUnit 5 tests across all modules.
- `./gradlew :opensearch-mcp-core:test`: run core unit tests while iterating on tool logic.
- `./gradlew :opensearch-mcp-http:runLocalJar`: build and run the HTTP server with the `local` Spring profile.
- `./gradlew :opensearch-mcp-stdio:runLocalJar`: build and run the stdio server locally.
- `docker compose up --build`: start the local OpenSearch cluster and HTTP MCP server.
- `./scripts/test-cluster-health.sh`: smoke-test the HTTP MCP endpoint after session initialization.

## Coding Style & Naming Conventions
Target Java 25 and match the existing `org.o8h.mcp.*` package layout. Use 4-space indentation, PascalCase for classes, lowerCamelCase for methods and fields, and one public type per file. Keep tool classes small and explicit; prefer straightforward Spring wiring over broad abstractions. Gradle files use Kotlin DSL and should follow surrounding formatting because no formatter is configured in the build.

## Testing Guidelines
Tests use JUnit 5, AssertJ, Spring test support, and Mockito where needed. Name test classes `*Test` and prefer descriptive method names such as `getAllocation_unknownCluster_returnsError`. Add or update tests with every behavior change. For local overrides, tests can read an optional repository-root `_test.yml`.

## Commit & Pull Request Guidelines
Recent history mostly follows conventional prefixes like `feat:`, `fix:`, `refactor:`, `build:`, `docs:`, and `chore:`. Keep commit subjects imperative and narrowly scoped, for example `fix: handle missing clusterName in allocation tool`. Pull requests should summarize the behavior change, list verification performed, and call out any config, transport, or auth impact. Include example requests or logs when changing MCP interaction flows.

## Security & Configuration Tips
Do not commit real cluster credentials or local-only overrides. Prefer environment variables such as `OPENSEARCH_USERNAME` and `OPENSEARCH_PASSWORD`, and use `.mcp.example.json` as the reference for local MCP client setup.
