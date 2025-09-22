plugins {
    id("org.gradlex.internal.plugin-publish-conventions") version "0.6"
}

group = "org.gradlex"
version = "1.1"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks.compileJava {
    options.release = 8
}

pluginPublishConventions {
    id("${project.group}.${project.name}")
    implementationClass("org.gradlex.javamodule.packaging.JavaModulePackagingPlugin")
    displayName("Java Module Packaging Gradle Plugin")
    description("A plugin to package Java Module applications for multiple platforms")
    tags("gradlex", "java", "modularity", "jigsaw", "jpms", "packaging", "jpackage")
    gitHub("https://github.com/gradlex-org/java-module-packaging")
    developer {
        id.set("jjohannes")
        name.set("Jendrik Johannes")
        email.set("jendrik@gradlex.org")
    }
}

testing.suites.named<JvmTestSuite>("test") {
    useJUnitJupiter()
    listOf("7.4", "7.6.5", "8.0.2", "8.14.2").forEach { gradleVersionUnderTest ->
        targets.register("test${gradleVersionUnderTest}") {
            testTask {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                description = "Runs tests against Gradle $gradleVersionUnderTest"
                systemProperty("gradleVersionUnderTest", gradleVersionUnderTest)
            }
        }
    }
    dependencies {
        implementation("org.assertj:assertj-core:3.27.6")
    }
    targets.configureEach {
        testTask {
            maxParallelForks = 4
        }
    }
}
