import org.gradle.api.tasks.testing.Test
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.o8h.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.spring.ai.bom))

    implementation(project(":opensearch-mcp-core"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.ai.starter.mcp.server.webmvc)

    annotationProcessor(platform(libs.spring.boot.dependencies))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    useJUnitPlatform {
        excludeTags("integration")
    }
    systemProperty("spring.config.additional-location", "optional:file:${rootProject.projectDir}/_test.yml")
}

val bootJarTask =
    tasks.named<BootJar>("bootJar") {
        destinationDirectory = rootProject.layout.projectDirectory.dir("build/libs")
    }

tasks.register<Exec>("runLocalJar") {
    group = "application"
    description = "Builds the HTTP boot jar and runs it with the local Spring profile."
    dependsOn(tasks.named("clean"), bootJarTask)
    workingDir = rootProject.projectDir

    doFirst {
        commandLine(
            "${System.getProperty("java.home")}/bin/java",
            "-jar",
            bootJarTask
                .get()
                .archiveFile
                .get()
                .asFile.absolutePath,
            "--spring.profiles.active=local",
        )
    }
}
