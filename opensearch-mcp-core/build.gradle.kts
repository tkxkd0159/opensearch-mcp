import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.o8h.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.spring.ai.bom))

    implementation(project(":opensearch-mcp-api"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.httpclient5)
    implementation(libs.spring.ai.starter.mcp.server.webmvc)

    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.config.additional-location", "optional:file:${rootProject.projectDir}/_test.yml")
}

tasks.named<BootJar>("bootJar") {
    destinationDirectory = rootProject.layout.projectDirectory.dir("build/libs")
}
