// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.gradlex.javamodule.packaging.test.fixture.GradleBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FatModuleJarTest {

    GradleBuild build = new GradleBuild();

    @BeforeEach
    void setup() {
        build.appBuildFile.appendText("""
            version = "1.0"
            dependencies {
                implementation("org.apache.commons:commons-csv:1.14.1")
            }
        """);
        build.appModuleInfoFile.writeText("""
            module org.example.app {
                requires org.apache.commons.codec;
                requires org.apache.commons.csv;
                requires org.apache.commons.io;
            }
        """);
        build.file("app/src/main/java/org/example/app/Main.java").writeText("""
            package org.example.app;
            public class Main {
                public static void main(String... args) {
                    boolean isInFatJar = Main.class.getProtectionDomain().getCodeSource().getLocation().toString()
                        .endsWith("app-1.0-all.jar!/modulepath/app-1.0/");
                    System.out.println(Main.class.getModule().getName() + " / " + isInFatJar);
                }
            }
            """);
    }

    @Test
    void offers_one_fat_jar_task_for_the_default_target() {
        build.build(":app:fatModuleJar");
        assertThat(build.projectDir.file("app/build/libs/app-1.0-all.jar").getAsPath())
                .isRegularFile();
    }

    @Test
    void offers_one_fat_jar_task_per_target() {
        var macOsJar =
                build.projectDir.file("app/build/libs/app-1.0-all-macos.jar").getAsPath();
        var windowsJar =
                build.projectDir.file("app/build/libs/app-1.0-all-windows.jar").getAsPath();
        var linuxJar =
                build.projectDir.file("app/build/libs/app-1.0-all-ubuntu.jar").getAsPath();

        var macosArch = System.getProperty("os.arch").contains("aarch") ? "aarch64" : "x86-64";
        build.appBuildFile.appendText("""
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

        build.build(":app:fatModuleJarMacos");
        assertThat(macOsJar).isRegularFile();

        build.build(":app:fatModuleJarWindows");
        assertThat(windowsJar).isRegularFile();

        build.build(":app:fatModuleJarUbuntu");
        assertThat(linuxJar).isRegularFile();

        assertThat(macOsJar).hasSize(1082397);
        assertThat(macOsJar).hasSameBinaryContentAs(windowsJar);
        assertThat(macOsJar).hasSameBinaryContentAs(linuxJar);
    }

    @Test
    void target_specific_fat_jars_differ_if_they_package_target_specific_modules() {
        var macOsJar =
                build.projectDir.file("app/build/libs/app-1.0-all-macos.jar").getAsPath();
        var windowsJar =
                build.projectDir.file("app/build/libs/app-1.0-all-windows.jar").getAsPath();
        var linuxJar =
                build.projectDir.file("app/build/libs/app-1.0-all-ubuntu.jar").getAsPath();

        build.appBuildFile.appendText("""
            dependencies {
                  implementation("org.openjfx:javafx-base:17")
            }
            jvmDependencyConflicts {
                patch {
                    module("org.openjfx:javafx-base") {
                        addTargetPlatformVariant("", "none", "none")
                        addTargetPlatformVariant("linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
                        addTargetPlatformVariant("linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
                        addTargetPlatformVariant("mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
                        addTargetPlatformVariant("mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
                        addTargetPlatformVariant("win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
                    }
                }
            }
        """);

        var macosArch = System.getProperty("os.arch").contains("aarch") ? "aarch64" : "x86-64";
        build.appBuildFile.appendText("""
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

        build.build(":app:fatModuleJarMacos");
        assertThat(macOsJar).isRegularFile();

        build.build(":app:fatModuleJarWindows");
        assertThat(windowsJar).isRegularFile();

        build.build(":app:fatModuleJarUbuntu");
        assertThat(linuxJar).isRegularFile();

        assertThat(macOsJar).hasSize(1894825);
        assertThat(windowsJar).hasSize(1883724);
        assertThat(linuxJar).hasSize(1886499);
    }

    @Test
    void fat_jar_runs() throws IOException, InterruptedException {
        build.build(":app:fatModuleJar");

        var fatJar = build.projectDir
                .file("app/build/libs/app-1.0-all.jar")
                .getAsPath()
                .toAbsolutePath()
                .toString();
        String[] command = {"java", "-jar", fatJar};
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            assertThat(line).isEqualTo("org.example.app / true");
        }
        assertThat(process.waitFor()).isEqualTo(0);
    }
}
