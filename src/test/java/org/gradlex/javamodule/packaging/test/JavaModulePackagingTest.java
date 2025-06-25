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

package org.gradlex.javamodule.packaging.test;

import org.assertj.core.api.Assertions;
import org.gradlex.javamodule.packaging.test.fixture.GradleBuild;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.hostOs;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnLinux;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnMacos;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnWindows;

class JavaModulePackagingTest {

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
        var taskToRun = ":app:assemble" + capitalize(label);
        var taskToCheck = ":app:jpackage" + capitalize(label);
        var macosArch = System.getProperty("os.arch").contains("aarch") ? "aarch64" : "x86-64";
        build.appBuildFile.appendText("""
                    version = "1.0"
                    javaModulePackaging {
                        target("macos") {
                            operatingSystem = "macos"
                            architecture = "%s"
                        }
                        target("ubuntu") {
                            operatingSystem = "linux"
                            architecture = "x86-64"
                        }
                        target("windows") {
                            operatingSystem = "windows"
                            architecture = "x86-64"
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
            Assertions.assertThat(buildResult.getOutput()).contains(
                    "> Running on %s; cannot build for %s".formatted(hostOs(), os));
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }

}
