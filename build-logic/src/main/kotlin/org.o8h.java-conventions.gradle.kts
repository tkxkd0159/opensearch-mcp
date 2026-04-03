import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
}

group = "org.o8h"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val libs = the<LibrariesForLibs>()

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
