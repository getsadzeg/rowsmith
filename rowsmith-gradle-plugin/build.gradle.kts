plugins {
    `java-gradle-plugin`
    `maven-publish`
}

description = "Gradle plugin: generates Rowsmith's typed table and column constants for the test source set."

dependencies {
    implementation(project(":rowsmith-codegen"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)
}

gradlePlugin {
    plugins {
        create("rowsmith") {
            id = "io.github.getsadzeg.rowsmith"
            implementationClass = "rowsmith.gradle.RowsmithPlugin"
            displayName = "Rowsmith"
            description = project.description
        }
    }
}

// TestKit builds compile generated code against rowsmith-core, and a custom schema source against rowsmith-codegen.
val coreJar = project(":rowsmith-core").tasks.named<Jar>("jar")
val codegenJar = project(":rowsmith-codegen").tasks.named<Jar>("jar")
tasks.test {
    dependsOn(coreJar, codegenJar)
    systemProperty("rowsmith.test.coreJar", coreJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("rowsmith.test.codegenJar", codegenJar.get().archiveFile.get().asFile.absolutePath)
}
