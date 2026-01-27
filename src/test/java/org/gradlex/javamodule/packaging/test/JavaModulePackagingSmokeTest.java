// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.test;

import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.hostOs;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnLinux;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnMacos;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnWindows;

import java.util.Locale;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.gradlex.javamodule.packaging.test.fixture.GradleBuild;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that run on all operating systems and assert success or failure depending on the system they run on.
 */
class JavaModulePackagingSmokeTest {

    GradleBuild build = new GradleBuild();

    private static Stream<Arguments> testTargets() {
        return Stream.of(
                Arguments.of("windows", "windows", runsOnWindows()),
                Arguments.of("macos", "macos", runsOnMacos()),
                Arguments.of("ubuntu", "linux", runsOnLinux()));
    }

    @ParameterizedTest
    @MethodSource("testTargets")
    void can_use_plugin(String label, String os, boolean success) {
        var taskToRun = ":app:jpackage" + capitalize(label);
        var taskToCheck = ":app:jpackage" + capitalize(label);
        var macosArch = System.getProperty("os.arch").contains("aarch") ? "aarch64" : "x86-64";
        build.appBuildFile.appendText("""
                    version = "1.0"
                    javaModulePackaging {
                        target("macos") {
                            operatingSystem.set("macos")
                            architecture.set("%s")
                        }
                        target("ubuntu") {
                            operatingSystem.set("linux")
                            architecture.set("x86-64")
                        }
                        target("windows") {
                            operatingSystem.set("windows")
                            architecture.set("x86-64")
                        }
                    }
                """.formatted(macosArch));
        build.appModuleInfoFile.writeText("""
                    module org.example.app {
                    }
                """);

        var buildResult = success ? build.build(taskToRun) : build.fail(taskToRun);
        var taskResult = buildResult.task(taskToCheck);

        Assertions.assertThat(taskResult).isNotNull();
        Assertions.assertThat(taskResult.getOutcome()).isEqualTo(success ? SUCCESS : FAILED);
        if (!success) {
            Assertions.assertThat(buildResult.getOutput())
                    .contains("> Running on %s; cannot build for %s".formatted(hostOs(), os));
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
