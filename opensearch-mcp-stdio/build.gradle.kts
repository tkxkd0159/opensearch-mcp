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

val bootJarTask = tasks.named<BootJar>("bootJar") {
    destinationDirectory = rootProject.layout.projectDirectory.dir("build/libs")
}

tasks.register<Exec>("runLocalJar") {
    group = "application"
    description = "Builds the stdio boot jar and runs it with the local Spring profile."
    dependsOn(tasks.named("clean"), bootJarTask)
    workingDir = rootProject.projectDir

    doFirst {
        commandLine(
            "${System.getProperty("java.home")}/bin/java",
            "-jar",
            bootJarTask.get().archiveFile.get().asFile.absolutePath,
            "--spring.profiles.active=local",
        )
    }
}
