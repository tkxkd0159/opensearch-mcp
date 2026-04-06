import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    id("org.o8h.java-conventions")
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.spring.ai.bom))

    implementation(libs.httpclient5)
    implementation(libs.spring.web)
    implementation(libs.spring.ai.starter.mcp.server)
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.opensearch.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
    systemProperty("spring.config.additional-location", "optional:file:${rootProject.projectDir}/_test.yml")
}

val testSourceSet = the<SourceSetContainer>()["test"]

tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers-backed integration tests for the shared core module."
    group = "verification"
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    shouldRunAfter(tasks.named("test"))
    useJUnitPlatform {
        includeTags("integration")
    }
    systemProperty("spring.config.additional-location", "optional:file:${rootProject.projectDir}/_test.yml")
}

tasks.named("check") {
    dependsOn("integrationTest")
}
