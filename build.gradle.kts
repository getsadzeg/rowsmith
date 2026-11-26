import com.vanniktech.maven.publish.MavenPublishBaseExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.plugin.publish) apply false
}

allprojects {
    group = "io.github.getsadzeg"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java")
    if (path.startsWith(":rowsmith-")) {
        apply(plugin = "com.vanniktech.maven.publish")
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            pom {
                name.set(project.name)
                description.set(provider { project.description })
                url.set("https://github.com/getsadzeg/rowsmith")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("getsadzeg")
                        name.set("Guri Getsadze")
                        url.set("https://github.com/getsadzeg")
                    }
                }
                scm {
                    url.set("https://github.com/getsadzeg/rowsmith")
                    connection.set("scm:git:git://github.com/getsadzeg/rowsmith.git")
                    developerConnection.set("scm:git:ssh://git@github.com/getsadzeg/rowsmith.git")
                }
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-parameters"))
    }

    if (path.startsWith(":rowsmith-")) {
        apply(plugin = "net.ltgt.errorprone")
        apply(plugin = "pmd")
        dependencies.add("errorprone", rootProject.libs.errorprone.core)
        tasks.withType<JavaCompile>().configureEach {
            options.errorprone {
                allErrorsAsWarnings = true
                disableWarningsInGeneratedCode = true
                excludedPaths = ".*/build/generated/.*|.*/src/test/java/.*/Db\\.java"
            }
        }
        extensions.configure<PmdExtension> {
            toolVersion = rootProject.libs.versions.pmd.get()
            ruleSets = listOf()
            ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
            isIgnoreFailures = true
            isConsoleOutput = true
        }
        tasks.withType<Pmd>().configureEach {
            exclude("**/Db.java")
        }
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
