import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
    jacoco
    alias(libs.plugins.spotless)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

spotless {
    java {
        target("**/src/*/java/**/*.java")
        targetExclude("**/build/**")
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
    }
}

val aggregateJavadoc by tasks.registering(Javadoc::class) {
    description = "Generates aggregated Javadoc for all Java modules."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    setDestinationDir(
        layout.buildDirectory
            .dir("docs/javadoc")
            .get()
            .asFile,
    )

    val standardOptions = options as StandardJavadocDocletOptions
    standardOptions.encoding = "UTF-8"
    standardOptions.charSet = "UTF-8"
    standardOptions.addBooleanOption("Xdoclint:all", true)
}

tasks.register("javadoc") {
    description = "Generates aggregated Javadoc for all Java modules."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    dependsOn(aggregateJavadoc)
}

tasks.named("check") {
    dependsOn("spotlessCheck")
    dependsOn("jacocoAggregateCoverageVerification")
    dependsOn(aggregateJavadoc)
}

val coverageExcludes = listOf("**/*Application.class")

val jacocoAggregateReport by tasks.registering(JacocoReport::class) {
    description = "Generates an aggregate JaCoCo coverage report for all Java modules."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
}

val jacocoAggregateCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    description = "Verifies aggregate line coverage for all Java modules."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
}

gradle.projectsEvaluated {
    val javaProjects = subprojects.filter { it.plugins.hasPlugin("java") }
    val mainSourceSets =
        javaProjects.map { project ->
            project.extensions
                .getByType<SourceSetContainer>()
                .named("main")
                .get()
        }
    val testTasks =
        javaProjects.map { project ->
            project.tasks.named("test")
        }
    val classesTasks =
        javaProjects.map { project ->
            project.tasks.named("classes")
        }
    val executionDataFiles =
        javaProjects.map { project ->
            project.layout.buildDirectory.file("jacoco/test.exec")
        }
    val classTrees =
        mainSourceSets.map { sourceSet ->
            sourceSet.output.asFileTree.matching {
                exclude(coverageExcludes)
            }
        }
    val sourceDirs = mainSourceSets.map { sourceSet -> sourceSet.allSource.srcDirs }

    aggregateJavadoc.configure {
        dependsOn(classesTasks)
        title = "${rootProject.name} API"
        classpath = files(mainSourceSets.map { it.compileClasspath }, mainSourceSets.map { it.output })
        mainSourceSets.forEach { sourceSet ->
            source(sourceSet.allJava)
        }
    }

    jacocoAggregateReport.configure {
        dependsOn(testTasks)
        classDirectories.setFrom(classTrees)
        sourceDirectories.setFrom(sourceDirs)
        additionalSourceDirs.setFrom(sourceDirs)
        executionData.setFrom(executionDataFiles)
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }

    jacocoAggregateCoverageVerification.configure {
        dependsOn(testTasks)
        classDirectories.setFrom(classTrees)
        sourceDirectories.setFrom(sourceDirs)
        additionalSourceDirs.setFrom(sourceDirs)
        executionData.setFrom(executionDataFiles)
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }
}
