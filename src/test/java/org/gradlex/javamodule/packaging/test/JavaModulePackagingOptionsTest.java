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

import org.gradlex.javamodule.packaging.test.fixture.GradleBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for setting various options for jpackage or the underlying jlink.
 * The tests are OS-dependent and should run on each operating system once.
 */
class JavaModulePackagingOptionsTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        var macosArch = System.getProperty("os.arch").contains("aarch") ? "aarch64" : "x86-64";
        build.appBuildFile.appendText("""
            version = "1.0"
            javaModulePackaging {
                target("macos") {
                    operatingSystem.set("macos")
                    architecture.set("%s")
                    packageTypes.set(listOf("dmg"))
                }
                target("ubuntu") {
                    operatingSystem.set("linux")
                    architecture.set("x86-64")
                    packageTypes.set(listOf("deb"))
                }
                target("windows") {
                    operatingSystem.set("windows")
                    architecture.set("x86-64")
                    packageTypes.set(listOf("exe"))
                }
            }
        """.formatted(macosArch));
        build.appModuleInfoFile.writeText("""              
            module org.example.app {
            }
        """);

    }

    @Test
    void can_configure_jlink_options() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                jlinkOptions.addAll(
                    "--ignore-signing-information",
                    "--compress", "zip-6",
                    "--no-header-files",
                    "--no-man-pages",
                    "--bind-services",
                    "--unsupported-option"
                )
            }
        """);

        var result = build.fail(":app:jpackage");

        // The error shows that all options before '--unsupported-option' are passed through to jlink
        assertThat(result.getOutput()).contains("jlink failed with: Error: unknown option: --unsupported-option");
    }

    @Test
    void can_configure_java_options() {
        build.appBuildFile.appendText("""
            application {
                applicationDefaultJvmArgs = listOf(
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseCompactObjectHeaders",
                    "-Xmx1g",
                    "-Dsome.prop=some.val"
                )
            }
        """);

        build.build(":app:jpackage");

        assertThat(build.appContentsFolder().file("app/app.cfg").getAsPath()).hasContent("""
            [Application]
            app.mainmodule=org.example.app/org.example.app.Main
            
            [JavaOptions]
            java-options=-Djpackage.app-version=1.0
            java-options=-XX:+UnlockExperimentalVMOptions
            java-options=-XX:+UseCompactObjectHeaders
            java-options=-Xmx1g
            java-options=-Dsome.prop=some.val
            """);
    }

    @Test
    void can_configure_add_modules() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                addModules.addAll("com.acme.boo")
            }
        """);

        var result = build.fail(":app:jpackage");

        // The error shows that the option is passed on to jlink
        assertThat(result.getOutput()).contains("jlink failed with: Error: Module com.acme.boo not found");
    }

    @Test
    void can_set_verbose_option() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                verbose.set(true)
            }
        """);

        var result = build.build(":app:jpackage");

        assertThat(result.getOutput()).contains("Creating app package: ");
    }

    @Test
    void can_set_target_specific_option() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                targetsWithOs("windows") {
                    singleStepPackaging.set(true)
                    options.addAll("--dummy")
                    appImageOptions.addAll("--dummyimg") // no effect due to single-step
                }
                targetsWithOs("linux") {
                    singleStepPackaging.set(true)
                    options.addAll("--dummy")
                    appImageOptions.addAll("--dummyimg") // no effect due to single-step
                }
                targetsWithOs("macos") {
                    singleStepPackaging.set(true)
                    options.addAll("--dummy")
                    appImageOptions.addAll("--dummyimg") // no effect due to single-step
                }
            }
        """);

        var result = build.fail(":app:jpackage");

        assertThat(result.getOutput()).contains("Error: Invalid Option: [--dummy]");
    }

    @Test
    void can_set_target_specific_option_for_app_image() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                targetsWithOs("windows") {
                    options.addAll("--dummy") // no effect as app-image fails first
                    appImageOptions.addAll("--dummyimg")
                }
                targetsWithOs("linux") {
                    options.addAll("--dummy") // no effect as app-image fails first
                    appImageOptions.addAll("--dummyimg")
                }
                targetsWithOs("macos") {
                    options.addAll("--dummy") // no effect as app-image fails first
                    appImageOptions.addAll("--dummyimg")
                }
            }
        """);

        var result = build.fail(":app:jpackage");

        assertThat(result.getOutput()).contains("Error: Invalid Option: [--dummyimg]");
    }

    @Test
    void can_build_package_in_one_step() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                targetsWithOs("windows") { singleStepPackaging.set(true) }
                targetsWithOs("linux") { singleStepPackaging.set(true) }
                targetsWithOs("macos") { singleStepPackaging.set(true) }
            }
        """);

        build.build(":app:jpackage");

        assertThat(build.appImageFolder().getAsPath()).isDirectoryContaining(f ->
                f.getFileName().toString().contains("app") && f.getFileName().toString().contains("1.0"));
        assertThat(build.appImageFolder().getAsPath()).isDirectoryNotContaining(f -> f.toFile().isDirectory());
    }
}
