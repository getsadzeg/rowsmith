plugins {
    `java-library`
}

description = "JUnit 5 extension: injects a Rowsmith bound to the test DataSource and cleans up after each test."

dependencies {
    api(project(":rowsmith-core"))
    implementation(libs.postgresql)
    compileOnly(libs.junit5.api)
    compileOnly(libs.testcontainers.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.junit.testkit)
    testImplementation(libs.assertj)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
    testRuntimeOnly(libs.slf4j.simple)
}
