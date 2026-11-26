plugins {
    `java-library`
}

description = "Rowsmith core: reads the live Postgres catalog and creates valid, connected rows."

dependencies {
    // Strings matching regex CHECKs
    implementation(libs.rgxgen)
    // Rowsmith only talks to Postgres: SQLSTATEs (PSQLState) and structured server errors
    implementation(libs.postgresql)
    // Optional: when Spring is on the classpath, inserts join the active Spring transaction.
    compileOnly(libs.spring.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.jdbc)
    testRuntimeOnly(libs.slf4j.simple)
}

tasks.test {
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    // Real-world schema coverage report lands here.
    systemProperty("rowsmith.test.reportDir", layout.buildDirectory.dir("reports/rowsmith").get().asFile.absolutePath)
}
