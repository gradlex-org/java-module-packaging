plugins {
    id("com.gradle.develocity") version "3.17.6"
}

dependencyResolutionManagement {
    repositories.mavenCentral()
}

rootProject.name = "java-module-packaging"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}
