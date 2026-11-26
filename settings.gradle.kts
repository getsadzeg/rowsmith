rootProject.name = "rowsmith"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("rowsmith-core", "rowsmith-junit", "rowsmith-codegen", "rowsmith-gradle-plugin")
include("examples:demo-app")
