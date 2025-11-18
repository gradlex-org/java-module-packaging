# Java Module Packaging Gradle plugin

[![Build Status](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Factions-badge.atrox.dev%2Fgradlex-org%2Fjava-module-packaging%2Fbadge%3Fref%3Dmain&style=flat)](https://actions-badge.atrox.dev/gradlex-org/java-module-packaging/goto?ref=main)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Forg%2Fgradlex%2Fjava-module-packaging%2Forg.gradlex.java-module-packaging.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/org.gradlex.java-module-packaging)

A Gradle plugin to package modular Java application as standalone bundles/installers for Windows, macOS and Linux with [jpackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html). 

This [GradleX](https://gradlex.org) plugin is maintained by me, [Jendrik Johannes](https://github.com/jjohannes).
I offer consulting and training for Gradle and/or the Java Module System - please [reach out](mailto:jendrik.johannes@gmail.com) if you are interested.
There is also my [YouTube channel](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE) on Gradle topics.

If you have a suggestion or a question, please [open an issue](https://github.com/gradlex-org/java-module-packaging/issues/new).

# Java Modules with Gradle

If you plan to build Java Modules with Gradle, you should consider using these plugins on top of Gradle core:

- [`id("org.gradlex.java-module-dependencies")`](https://github.com/gradlex-org/java-module-dependencies)  
  Avoid duplicated dependency definitions and get your Module Path under control
- [`id("org.gradlex.jvm-dependency-conflict-resolution")`](https://github.com/gradlex-org/jvm-dependency-conflict-resolution)  
  Additional metadata for widely-used modules and patching facilities to add missing metadata
- [`id("org.gradlex.java-module-testing")`](https://github.com/gradlex-org/java-module-testing)  
  Proper test setup for Java Modules
- [`id("org.gradlex.extra-java-module-info")`](https://github.com/gradlex-org/extra-java-module-info)  
  Only if your (existing) project cannot avoid using non-module legacy Jars
- [`id("org.gradlex.java-module-packaging")`](https://github.com/gradlex-org/java-module-packaging)  
  Package standalone applications for Windows, macOS and Linux

[In episodes 31, 32, 33 of Understanding Gradle](https://github.com/jjohannes/understanding-gradle) I explain what these plugins do and why they are needed.
[<img src="https://onepiecesoftware.github.io/img/videos/31.png" width="260">](https://www.youtube.com/watch?v=X9u1taDwLSA&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/32.png" width="260">](https://www.youtube.com/watch?v=T9U0BOlVc-c&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)
[<img src="https://onepiecesoftware.github.io/img/videos/33.png" width="260">](https://www.youtube.com/watch?v=6rFEDcP8Noc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

[Full Java Module System Project Setup](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system) is a full-fledged Java Module System project setup using these plugins.  
[<img src="https://onepiecesoftware.github.io/img/videos/15-3.png" width="260">](https://www.youtube.com/watch?v=uRieSnovlVc&list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE)

# How to use?

Working example projects to inspect:
- [java-module-system](https://github.com/jjohannes/java-module-system) contains a compact sample and further documentation
- [gradle-project-setup-howto](https://github.com/jjohannes/gradle-project-setup-howto/tree/java_module_system) is a full-fledged Java Module System project setup

For general information about how to structure Gradle builds and apply community plugins like this one to all subprojects
you can check out my [Understanding Gradle video series](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl2k2OmC_gsjDpdIBTN0qqkE).

## Plugin dependency

Add this to the build file of your convention plugin's build
(e.g. `build-logic/build.gradle(.kts)` or `buildSrc/build.gradle(.kts)`).

```
dependencies {
    implementation("org.gradlex:java-module-packaging:1.2")
}
```

## Apply and use the plugin

In your convention plugin, apply the plugin and configure the _targets_.

```
plugins {
    id("org.gradlex.java-module-packaging")
}

javaModulePackaging {
    target("ubuntu-22.04") {
        operatingSystem = OperatingSystemFamily.LINUX
        architecture = MachineArchitecture.X86_64
    }
    target("macos-13") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.X86_64
    }
    target("macos-14") {
        operatingSystem = OperatingSystemFamily.MACOS
        architecture = MachineArchitecture.ARM64
    }
    target("windows-2022") {
        operatingSystem = OperatingSystemFamily.WINDOWS
        architecture = MachineArchitecture.X86_64
    }
}
```

You can now run _target-specific_ builds:

```shell
./gradlew jpackageWindows
```

```shell
./gradlew runWindows
```

Or, for convenience, let the plugin pick the target fitting the machine you run on:

```shell
./gradlew jpackage
```

```shell
./gradlew run
```

There are some additional configuration options that can be used if needed.
All options have a default. Only configure what you need in addition.
For more information about the available options, consult the
[jpackage](https://docs.oracle.com/en/java/javase/24/docs/specs/man/jpackage.html) and
[jlink](https://docs.oracle.com/en/java/javase/24/docs/specs/man/jlink.html)
(for `jlinkOptions`) documentation.

```kotlin
javaModulePackaging {
  // global options
  applicationName = "app" // defaults to project name
  applicationVersion = "1.0" // defaults to project version
  applicationDescription = "Awesome App"
  vendor = "My Company" 
  copyright = "(c) My Company" 
  jlinkOptions.addAll("--no-header-files", "--no-man-pages", "--bind-services")
  addModules.addAll("additional.module.to.include")
  jpackageResources = layout.projectDirectory.dir("res") // defaults to 'src/main/resourcesPackage'
  resources.from(layout.projectDirectory.dir("extra-res"))
  verbose = false

  // target specific options
  targetsWithOs("windows") {
    options.addAll("--win-dir-chooser", "--win-shortcut", "--win-menu")
    appImageOptions.addAll("--win-console")
    targetResources.from("windows-res")
  }
  targetsWithOs("macos") {
    options.addAll("--mac-sign", "--mac-signing-key-user-name", "gradlex")
    singleStepPackaging = true
  }
}
```

## Using target specific variants of libraries (like JavaFX)

The plugin uses Gradle's [variant-aware dependency management](https://docs.gradle.org/current/userguide/variant_model.html)
to select target-specific Jars based on the configured [targets](#apply-and-use-the-plugin).
For this, such a library needs to be published with [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html)
and contain the necessary information about the available target-specific Jars.
If the metadata is missing or incomplete, you should use the [org.gradlex.jvm-dependency-conflict-resolution](https://github.com/gradlex-org/jvm-dependency-conflict-resolution)
plugin to add the missing information via [addTargetPlatformVariant](https://gradlex.org/jvm-dependency-conflict-resolution/#patch-dsl-block).

For example, for JavaFX it may look like this:
```
jvmDependencyConflicts.patch {
  listOf("base", "graphics", "controls").forEach { jfxModule ->
    module("org.openjfx:javafx-$jfxModule") {
      addTargetPlatformVariant("linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
      addTargetPlatformVariant("linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
      addTargetPlatformVariant("mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
      addTargetPlatformVariant("mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
      addTargetPlatformVariant("win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
    }
  }   
}
```

## Testing against multiple targets

> [!WARNING]
> Currently, the following only works in combination with [Blackbox Test Suites configured by the _org.gradlex.java-module-testing_ plugin](https://github.com/gradlex-org/java-module-testing?tab=readme-ov-file#blackbox-test-suites).

Tests run against the _primary_ target, which is either the local machine you run the build on, or what is configured via `javaModulePackaging.primaryTarget(...)`.
If you want to run the test multiple times against each target you configured, you can configure this as follows:

```
javaModulePackaging {
    multiTargetTestSuite(testing.suites["test"])
}
```

Then, there will be a test task available for each target, such as `testWindows-2022` or `testMacos-14`.

## Running on GitHub Actions

Target-specific _tasks_ such as `assembleWindows-2022` or `assembleMacos-14` only run on the fitting operating system and architecture.
If you want to build your software for multiple targets and have GitHub actions available, you can use different
runners to create packages for the different targets. A setup for this can look like this
(assuming your targets are named: `ubuntu-22.04`, `windows-2022`, `macos-13`, `macos-14`):

```
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version-file: ./gradle/java-version.txt
      - uses: gradle/actions/setup-gradle@v3
      - run: "./gradlew check"

  package:
    needs: check
    strategy:
      matrix:
        os: [ubuntu-22.04, windows-2022, macos-13, macos-14]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version-file: ./gradle/java-version.txt
      - uses: gradle/actions/setup-gradle@v3
      - run: "./gradlew assemble${{ matrix.os }}"
      - uses: actions/upload-artifact@v4
        with:
          name: Application Package ${{ matrix.os }}
          path: build/app/packages/*/*
```

To avoid re-compilation of the Java code on each of the runners, you can run a
[Gradle remote cache node](https://docs.gradle.com/build-cache-node).

The [java-module-system](https://github.com/jjohannes/java-module-system) project is an example that
uses GitHub actions with a Gradle remote build cache.

## FAQ

### How does the plugin interact with the `jpackage` command?

By default, dhe plugin calls `jpackage` in two steps:

1. Build `--type app-image` as a package-type independent image folder. This is where `jlink` is involved.
2. Build OS-specific packages via `--type <package-type>`.
   This may be called several times for the same target (e.g. `exe` and `msi` on Windows).

OS-independent options can be configured through the extension:

```kotlin
javaModulePackaging {
  applicationName = "app" // defaults to project name
  applicationVersion = "1.0" // defaults to project version
  applicationDescription = "Awesome App"
  vendor = "My Company" 
  copyright = "(c) My Company" 
  jlinkOptions.addAll("--no-header-files", "--no-man-pages", "--bind-services")
  addModules.addAll("additional.module.to.include")
  verbose = false
}
```

OS-specific options can be defined inside a target:

```kotlin
javaModulePackaging {
  target("windows-2022") { // address target by name
    options.addAll("--win-dir-chooser", "--win-shortcut", "--win-menu")
  }
  targetsWithOs("windows") { // all targets of for a certain os
    // ...
  }
}
```

You can tell the plugin to perform packaging in one step by setting the `singleStepPackaging = true` option on a target.

# Disclaimer

Gradle and the Gradle logo are trademarks of Gradle, Inc.
The GradleX project is not endorsed by, affiliated with, or associated with Gradle or Gradle, Inc. in any way.
