/*
 * Copyright the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.javamodule.packaging.test.fixture;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class GradleBuild {

    public final Directory projectDir;
    public final WritableFile settingsFile;
    public final WritableFile appBuildFile;
    public final WritableFile libBuildFile;
    public final WritableFile appModuleInfoFile;
    public final WritableFile libModuleInfoFile;

    public static final String GRADLE_VERSION_UNDER_TEST = System.getProperty("gradleVersionUnderTest");

    public GradleBuild() {
        this(createBuildTmpDir());
    }

    public GradleBuild(Path dir) {
        this.projectDir = new Directory(dir);
        this.settingsFile = file("settings.gradle.kts");
        this.appBuildFile = file("app/build.gradle.kts");
        this.appModuleInfoFile = file("app/src/main/java/module-info.java");
        this.libBuildFile = file("lib/build.gradle.kts");
        this.libModuleInfoFile = file("lib/src/main/java/module-info.java");

        settingsFile.writeText("""
            dependencyResolutionManagement { repositories.mavenCentral() }
            includeBuild(".")
            rootProject.name = "test-project"
            include("app", "lib")
        """);
        appBuildFile.writeText("""
            plugins {
                id("org.gradlex.java-module-packaging")
                id("application")
            }
            group = "org.example"
            java {
                toolchain.languageVersion.set(JavaLanguageVersion.of(17))
            }
            application {
                mainModule.set("org.example.app")
                mainClass.set("org.example.app.Main")
            }
        """);
        file("app/src/main/java/org/example/app/Main.java").writeText("""
            package org.example.app;
            
            public class Main {
                public static void main(String... args) {
                }
            }
            """);
        file("app/src/test/java/org/example/app/test/MainTest.java").writeText("""
            package org.example.app.test;
            
            import org.junit.jupiter.api.Test;
            import org.example.app.Main;
            
            public class MainTest {
                @Test
                void testApp() {
                    new Main();
                }
            }
            """);

        libBuildFile.writeText("""
            plugins {
                id("org.gradlex.java-module-packaging")
                id("java-library")
            }
            group = "org.example"
            """);
    }

    public WritableFile file(String path) {
        return new WritableFile(projectDir, path);
    }

    public Directory appImageFolder() {
        if (runsOnMacos()) return projectDir.dir("app/build/packages/macos");
        if (runsOnLinux()) return projectDir.dir("app/build/packages/ubuntu");
        if (runsOnWindows()) return projectDir.dir("app/build/packages/windows");
        throw new IllegalStateException("unknown os");
    }

    public Directory appContentsFolder() {
        if (runsOnMacos()) return projectDir.dir("app/build/packages/macos/app.app/Contents");
        if (runsOnLinux()) return projectDir.dir("app/build/packages/ubuntu/app/lib");
        if (runsOnWindows()) return projectDir.dir("app/build/packages/windows/app");
        throw new IllegalStateException("unknown os");
    }

    public BuildResult build(String task) {
        return runner(task).build();
    }

    public BuildResult fail(String task) {
        return runner(task).buildAndFail();
    }

    public GradleRunner runner(String... args) {
        return runner(true, args);
    }

    public GradleRunner runner(boolean projectIsolation, String... args) {
        boolean debugMode = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
        List<String> latestFeaturesArgs = GRADLE_VERSION_UNDER_TEST != null || !projectIsolation ? List.of() : List.of(
                "--configuration-cache",
                "-Dorg.gradle.unsafe.isolated-projects=true"
        );
        Stream<String> standardArgs = Stream.of(
                "-s",
                "--warning-mode=all"
        );
        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withDebug(debugMode)
                .withProjectDir(projectDir.getAsPath().toFile())
                .withArguments(Stream.of(Arrays.stream(args), latestFeaturesArgs.stream(), standardArgs)
                        .flatMap(identity()).collect(Collectors.toList()));
        if (GRADLE_VERSION_UNDER_TEST != null) {
            runner.withGradleVersion(GRADLE_VERSION_UNDER_TEST);
        }
        return runner;
    }

    private static Path createBuildTmpDir() {
        try {
            return Files.createTempDirectory("gradle-build");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String currentTarget() {
        if (runsOnMacos()) return "macos";
        if (runsOnLinux()) return "ubuntu";
        if (runsOnWindows()) return "windows";
        throw new IllegalStateException("unknown os");
    }

    public static boolean runsOnWindows() {
        return hostOs().contains("win");
    }

    public static boolean runsOnMacos() {
        return hostOs().contains("mac");
    }

    public static boolean runsOnLinux() {
        return !runsOnWindows() && !runsOnMacos();
    }

    public static String hostOs() {
        String hostOs = System.getProperty("os.name").toLowerCase().replace(" ", "");
        if (hostOs.startsWith("mac")) return "macos";
        if (hostOs.startsWith("win")) return "windows";
        return "linux";
    }
}
