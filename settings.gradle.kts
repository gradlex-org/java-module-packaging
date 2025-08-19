plugins {
    id("com.gradle.develocity") version "4.1.1"
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
