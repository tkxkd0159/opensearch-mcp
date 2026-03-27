pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "opensearch-mcp"
include("opensearch-mcp-core", "opensearch-mcp-http", "opensearch-mcp-stdio")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
