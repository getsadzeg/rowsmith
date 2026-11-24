plugins {
    `java-library`
}

description = "Generates typed table and column constants (Db.java) for Rowsmith from a Postgres schema."

dependencies {
    api(project(":rowsmith-core"))
    implementation(libs.javapoet)
    implementation(libs.postgresql)
    implementation(libs.testcontainers.postgresql)
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.test {
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
}

// Golden files: Db classes generated from rowsmith-core's test schemas are checked in there and used by its tests.
val coreTests = project(":rowsmith-core").file("src/test")
tasks.test {
    inputs.files(fileTree(coreTests) { include("resources/*.sql", "java/**/Db.java") })
    systemProperty("rowsmith.test.coreTests", coreTests.absolutePath)
    // ./gradlew :rowsmith-codegen:test -PupdateGolden rewrites the golden files instead of comparing
    systemProperty("rowsmith.test.updateGolden", providers.gradleProperty("updateGolden").isPresent)
}
