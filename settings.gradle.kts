plugins {
    id("com.gradle.enterprise") version "3.17.5"
}

dependencyResolutionManagement {
    repositories.mavenCentral()
}

rootProject.name = "java-module-packaging"

gradleEnterprise {
    val runsOnCI = providers.environmentVariable("CI").getOrElse("false").toBoolean()
    if (runsOnCI) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
