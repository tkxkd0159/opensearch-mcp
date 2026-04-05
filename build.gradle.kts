import org.gradle.api.tasks.SourceSetContainer
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
        googleJavaFormat("1.33.0")
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

tasks.named("check") {
    dependsOn("spotlessCheck")
    dependsOn("jacocoAggregateCoverageVerification")
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
