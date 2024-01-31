package org.gradlex.javamodule.packaging.test.fixture

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import java.lang.management.ManagementFactory
import java.nio.file.Files

class GradleBuild {

    final File projectDir
    final File settingsFile
    final File appBuildFile
    final File appModuleInfoFile
    final File libBuildFile
    final File libModuleInfoFile

    final String gradleVersionUnderTest = System.getProperty('gradleVersionUnderTest')

    GradleBuild(File projectDir = Files.createTempDirectory('gradle-build').toFile()) {
        this.projectDir = projectDir
        this.settingsFile = file('settings.gradle.kts')
        this.appBuildFile = file('app/build.gradle.kts')
        this.appModuleInfoFile = file('app/src/main/java/module-info.java')
        this.libBuildFile = file('lib/build.gradle.kts')
        this.libModuleInfoFile = file('lib/src/main/java/module-info.java')

        file('app/src/main/resourcesPackage/windows').mkdirs()
        file('app/src/main/resourcesPackage/macos').mkdirs()
        file('app/src/main/resourcesPackage/linux').mkdirs()

        settingsFile << '''
            dependencyResolutionManagement { repositories.mavenCentral() }
            includeBuild(".")
            rootProject.name = "test-project"
            include("app", "lib")
        '''
        appBuildFile << '''
            plugins {
                id("org.gradlex.java-module-packaging")
                id("application")
            }
            group = "org.example"
            application {
                mainModule.set("org.example.app")
                mainClass.set("org.example.app.Main")
            }
        '''
        file("app/src/main/java/org/example/app/Main.java") << '''
            package org.example.app;
            
            public class Main {
                public static void main(String... args) {
                }
            }
        '''
        file("app/src/test/java/org/example/app/test/MainTest.java") << '''
            package org.example.app.test;
            
            import org.junit.jupiter.api.Test;
            import org.example.app.Main;
            
            public class MainTest {
                
                @Test
                void testApp() {
                    new Main();
                }
            }
        '''

        libBuildFile << '''
            plugins {
                id("org.gradlex.java-module-packaging")
                id("java-library")
            }
            group = "org.example"
        '''
    }

    File file(String path) {
        new File(projectDir, path).tap {
            it.getParentFile().mkdirs()
        }
    }

    static boolean runsOnWindows() {
        hostOs().contains('win')
    }

    static boolean runsOnMacos() {
        hostOs().contains('mac')
    }

    static boolean runsOnLinux() {
        !runsOnWindows() && !runsOnMacos()
    }

    static String hostOs() {
        System.getProperty("os.name").replace(" ", "").toLowerCase()
    }

    BuildResult build(taskToRun) {
        runner(taskToRun).build()
    }

    BuildResult fail(taskToRun) {
        runner(taskToRun).buildAndFail()
    }

    GradleRunner runner(String... args) {
        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments(Arrays.asList(args) + '-s')
                .withDebug(ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp")).with {
            gradleVersionUnderTest ? it.withGradleVersion(gradleVersionUnderTest) : it
        }
    }
}
