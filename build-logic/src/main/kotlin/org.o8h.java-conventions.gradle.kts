import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import javax.inject.Inject

plugins {
    java
    jacoco
    id("net.ltgt.errorprone")
}

group = "org.o8h"
version = "latest-SNAPSHOT"

val libs = the<LibrariesForLibs>()

abstract class MockitoAgentArgumentProvider
    @Inject
    constructor(
        @get:Classpath val testClasspath: FileCollection,
    ) : CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> {
            val mockitoCoreJar =
                testClasspath.files.firstOrNull { it.name.startsWith("mockito-core-") && it.extension == "jar" }
                    ?: return emptyList()
            return listOf("-javaagent:${mockitoCoreJar.absolutePath}")
        }
    }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = libs.versions.jacoco.get()
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-parameters")
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    options.errorprone.check("NullAway", CheckSeverity.ERROR)
    options.errorprone.option("NullAway:OnlyNullMarked", "true")
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("-parameters")
    options.errorprone.disableAllChecks.set(true)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgumentProviders.add(objects.newInstance<MockitoAgentArgumentProvider>(classpath))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.withType<Javadoc>().configureEach {
    val standardOptions = options as StandardJavadocDocletOptions
    standardOptions.encoding = "UTF-8"
    standardOptions.charSet = "UTF-8"
    standardOptions.addBooleanOption("Xdoclint:all", true)
}

tasks.named("check") {
    dependsOn(tasks.named("javadoc"))
}

dependencies {
    compileOnly(libs.jspecify)
    testCompileOnly(libs.jspecify)
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}
