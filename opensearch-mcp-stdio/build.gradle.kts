import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.o8h.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.spring.ai.bom))

    implementation(project(":opensearch-mcp-core"))
    implementation(libs.spring.ai.starter.mcp.server)

    annotationProcessor(platform(libs.spring.boot.dependencies))

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    destinationDirectory = rootProject.layout.projectDirectory.dir("build/libs")
}
