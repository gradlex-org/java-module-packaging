plugins {
    id("groovy")
    id("org.gradlex.internal.plugin-publish-conventions") version "0.6"
}

group = "org.gradlex"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation("org.spockframework:spock-core:2.1-groovy-3.0")
    testImplementation("org.gradle.exemplar:samples-check:1.0.3")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
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

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 4
}
