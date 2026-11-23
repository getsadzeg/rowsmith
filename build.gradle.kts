plugins {
    `java-library`
}

allprojects {
    group = "io.github.getsadzeg"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-parameters"))
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Postgres image for Testcontainers-based tests: -Ppg=16 -> postgres:16-alpine
        val pg = providers.gradleProperty("pg").orElse("17")
        systemProperty("rowsmith.test.pgImage", pg.map { if (it.contains(":")) it else "postgres:$it-alpine" }.get())
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        maxHeapSize = "1g"
    }
}
