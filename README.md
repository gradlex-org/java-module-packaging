# Java Module Packaging Gradle plugin

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fgradlex-org%2Fjava-module-packaging%2Fbadge%3Fref%3Dmain&style=flat)](https://actions-badge.atrox.dev/gradlex-org/java-module-packaging/goto?ref=main)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Forg%2Fgradlex%2Fjava-module-packaging%2Forg.gradlex.java-module-packaging.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/org.gradlex.java-module-packaging)

A Gradle plugin to package modular Java application as standalone bundles/installers for Windows, macOS and Linux with [jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html). 

This plugin is maintained by me, [Jendrik Johannes](https://github.com/jjohannes).
I offer consulting and training for Gradle and/or the Java Module System - please [reach out](mailto:jendrik.johannes@gmail.com) if you are interested.
There is also my [YouTube channel](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE) on Gradle topics.

If you have a suggestion or a question, please [open an issue](https://github.com/gradlex-org/java-module-packaging/issues/new).

# Java Modules with Gradle

If you plan to build Java Modules with Gradle, you should consider using these plugins on top of Gradle core:

- [`id("org.gradlex.java-module-dependencies")`](https://github.com/gradlex-org/java-module-dependencies)  
  Avoid duplicated dependency definitions and get your Module Path under control
- [`id("org.gradlex.java-module-testing")`](https://github.com/gradlex-org/java-module-testing)  
  Proper test setup for Java Modules
- [`id("org.gradlex.extra-java-module-info")`](https://github.com/gradlex-org/extra-java-module-info)  
  Only if your (existing) project cannot avoid using non-module legacy Jars
- [`id("org.gradlex.java-module-packaging")`](https://github.com/gradlex-org/java-module-packaging)  
  Package standalone applications for Windows, macOS and Linux

[Here is a sample](https://github.com/gradlex-org/java-module-testing/tree/main/samples/use-all-java-module-plugins)
that shows all plugins in combination.

[In episodes 31, 32, 33 of Understanding Gradle](https://github.com/jjohannes/understanding-gradle) I explain what these plugins do and why they are needed.
[<img src="https://onepiecesoftware.github.io/img/videos/31.png" width="260">](https://www.youtube.com/watch?v=X9u1taDwLSA&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/32.png" width="260">](https://www.youtube.com/watch?v=T9U0BOlVc-c&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/33.png" width="260">](https://www.youtube.com/watch?v=6rFEDcP8Noc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

[Full Java Module System Project Setup](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system) is a full-fledged Java Module System project setup using these plugins.  
[<img src="https://onepiecesoftware.github.io/img/videos/15-3.png" width="260">](https://www.youtube.com/watch?v=uRieSnovlVc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

# How to use?

For general information about how to structure Gradle builds and apply community plugins like this one to all subprojects
you can check out my [Understanding Gradle video series](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE).

## Plugin dependency

Add this to the build file of your convention plugin's build
(e.g. `build-logic/build.gradle(.kts)` or `buildSrc/build.gradle(.kts)`).

```
dependencies {
    implementation("org.gradlex:java-module-packaging:0.1")
}
```

## Apply and use the plugin

In your convention plugin, apply the plugin and configure the _targets_.

```
plugins {
    id("org.gradlex.java-module-packaging")
}

javaModulePackaging {
    target("windows") {
        operatingSystem = "windows"
        architecture = "x86-64"
    }
    target("ubuntu") {
        operatingSystem = "linux"
        architecture = "x86-64"
    }
    target("macos") {
        operatingSystem = "macos"
        architecture = "x86-64"
    }
    target("macosM1") {
        operatingSystem = "macos"
        architecture = "aarch64"
    }
}
```

## How ot use?

You can now run _target_ specific builds:

```
./gradlew assembleWindows
```

## Using target specific variants of libraries (like JavaFX)

## Running on GitHub Actions

# Disclaimer

Gradle and the Gradle logo are trademarks of Gradle, Inc.
The GradleX project is not endorsed by, affiliated with, or associated with Gradle or Gradle, Inc. in any way.
