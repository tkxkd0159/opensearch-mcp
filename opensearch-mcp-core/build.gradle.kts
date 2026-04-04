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
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.config.additional-location", "optional:file:${rootProject.projectDir}/_test.yml")
}
