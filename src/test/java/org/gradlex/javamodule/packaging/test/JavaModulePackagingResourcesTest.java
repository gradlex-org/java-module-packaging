// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.currentTarget;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnLinux;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnMacos;
import static org.gradlex.javamodule.packaging.test.fixture.GradleBuild.runsOnWindows;

import org.gradlex.javamodule.packaging.test.fixture.GradleBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for adding custom resources to the image/package.
 * The tests are OS-dependent and should run on each operating system once.
 */
class JavaModulePackagingResourcesTest {

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

        // The error shows that all options before '--unsupported-option' are passed through to jlink
        var result = build.fail(":app:jpackage");
        assertThat(result.getOutput()).contains("jlink failed with: Error: unknown option: --unsupported-option");
    }

    @Test
    void can_configure_add_modules() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                addModules.addAll("com.acme.boo")
            }
        """);

        // The error shows that the option is passed on to jlink
        var result = build.fail(":app:jpackage");
        assertThat(result.getOutput()).contains("jlink failed with: Error: Module com.acme.boo not found");
    }

    @Test
    void can_add_resources_for_jpackage() {
        // Use 'src/main/resourcesPackage', which is the convention

        // resources that are not known - will be ignored
        build.projectDir.file("app/src/main/resourcesPackage/linux/dummy.txt").writeText("");
        build.projectDir.file("app/src/main/resourcesPackage/macos/dummy.txt").writeText("");
        build.projectDir.file("app/src/main/resourcesPackage/windows/dummy.txt").writeText("");

        // icons will be used
        build.projectDir.file("app/src/main/resourcesPackage/linux/icon.png").create();
        build.projectDir.file("app/src/main/resourcesPackage/macos/icon.icns").create();
        build.projectDir.file("app/src/main/resourcesPackage/windows/icon.ico").create();

        build.build(":app:jpackage");

        String icon = "app.icns";
        if (runsOnLinux()) icon = "app.png";
        if (runsOnWindows()) icon = "app.icoxxx";

        // Intermediate location to collect files
        assertThat(build.file("app/build/tmp/jpackage/%s/jpackage-resources/dummy.txt".formatted(currentTarget()))
                        .getAsPath())
                .exists();
        assertThat(build.file("app/build/tmp/jpackage/%s/jpackage-resources/%s".formatted(currentTarget(), icon))
                        .getAsPath())
                .exists();

        // icons end up in Resources
        String resourcesFolder = "";
        if (runsOnMacos()) resourcesFolder = "Resources/";
        assertThat(build.appContentsFolder().file(resourcesFolder + icon).getAsPath())
                .hasSize(0);
    }

    @Test
    void can_add_resources_for_app_folder() {
        build.appBuildFile.appendText("""
            javaModulePackaging {
                // resource is added to the os-specific 'app' folder inside the image
                resources.from("res")
            }
        """);

        // resources that are not known - will be ignored
        build.projectDir.file("app/res/dummy.txt").writeText("");

        build.build(":app:jpackage");

        assertThat(build.appContentsFolder().file("app/dummy.txt").getAsPath()).exists();
    }

    @Test
    void can_add_resources_to_image_root() {
        // Resource is added to the root of the image.
        // This is a target-specific setting as it usually needs to be placed in a place that
        // makes sense in the corresponding package structure.
        build.appBuildFile.appendText("""
            javaModulePackaging {
                targetsWithOs("windows") { targetResources.from("res") }
                targetsWithOs("linux") { targetResources.from("res") }
                targetsWithOs("macos") { targetResources.from("res") }
            }
        """);

        // resources that are not known - will be ignored
        build.projectDir.file("app/res/customFolder/dummy.txt").writeText("");

        build.build(":app:jpackage");

        assertThat(build.appContentsFolder().file("customFolder/dummy.txt").getAsPath())
                .exists();
    }
}
