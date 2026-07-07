version = "1.3"

publishingConventions {
    pluginPortal("${project.group}.${project.name}") {
        implementationClass("org.gradlex.javamodule.packaging.JavaModulePackagingPlugin")
        displayName("Java Module Packaging Gradle Plugin")
        description("A plugin to package Java Module applications for multiple platforms")
        tags("gradlex", "java", "modularity", "jigsaw", "jpms", "packaging", "jpackage")
    }
    gitHub("https://github.com/gradlex-org/java-module-packaging")
    developer {
        id.set("jjohannes")
        name.set("Jendrik Johannes")
        email.set("jendrik@gradlex.org")
    }
}

testingConventions { testGradleVersions("7.4", "7.6.5", "8.0.2", "8.14.2") }

tasks.withType<Test>().configureEach {
    inputs.property("operatingSystemName", System.getProperty("os.name"))
    inputs.property("operatingSystemArch", System.getProperty("os.arch"))
    environment["JAVA_25"] =
        javaToolchains
            .launcherFor { languageVersion = JavaLanguageVersion.of(25) }
            .get()
            .metadata
            .installationPath
            .asFile
            .absolutePath
}
