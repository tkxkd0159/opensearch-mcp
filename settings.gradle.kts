pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "opensearch-mcp"
include("opensearch-mcp-api", "opensearch-mcp-core")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
