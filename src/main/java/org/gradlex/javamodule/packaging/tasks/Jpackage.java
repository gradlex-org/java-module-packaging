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

package org.gradlex.javamodule.packaging.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaInstallationMetadata;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.gradle.nativeplatform.OperatingSystemFamily.WINDOWS;
import static org.gradlex.javamodule.packaging.internal.Validator.validateHostSystem;

@CacheableTask
abstract public class Jpackage extends DefaultTask {

    @Nested
    abstract public Property<JavaInstallationMetadata> getJavaInstallation();

    @Input
    abstract public Property<String> getOperatingSystem();

    @Input
    abstract public Property<String> getArchitecture();

    @Input
    abstract public Property<String> getMainModule();

    @Input
    abstract public Property<String> getVersion();

    @Classpath
    abstract public ConfigurableFileCollection getModulePath();

    @Input
    abstract public Property<String> getApplicationName();

    @Input
    @Optional
    abstract public Property<String> getApplicationDescription();

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract public DirectoryProperty getJpackageResources();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract public ConfigurableFileCollection getResources();

    @Input
    @Optional
    abstract public Property<String> getVendor();

    @Input
    @Optional
    abstract public Property<String> getCopyright();

    @Input
    abstract public ListProperty<String> getJavaOptions();

    @Input
    abstract public ListProperty<String> getJlinkOptions();

    @Input
    abstract public ListProperty<String> getAddModules();

    @Input
    abstract public ListProperty<String> getOptions();

    @Input
    abstract public ListProperty<String> getPackageTypes();

    @Input
    abstract public Property<Boolean> getVerbose();

    @OutputDirectory
    abstract public DirectoryProperty getDestination();

    @Internal
    abstract public DirectoryProperty getTempDirectory();

    @Inject
    abstract protected FileOperations getFiles();

    @Inject
    abstract protected ExecOperations getExec();

    @TaskAction
    public void runJpackage() throws Exception {
        getFiles().delete(getTempDirectory());
        getFiles().delete(getDestination());

        String os = getOperatingSystem().get();
        String arch = getArchitecture().get();

        validateHostSystem(arch, os);

        Directory resourcesDir = getTempDirectory().get().dir("jpackage-resources");
        Directory appImageParent = getTempDirectory().get().dir("app-image");
        //noinspection ResultOfMethodCallIgnored
        resourcesDir.getAsFile().mkdirs();

        getFiles().copy(c -> {
            c.from(getJpackageResources());
            c.into(resourcesDir);
            c.rename(f -> f.replace("icon", getApplicationName().get()));
        });

        String executableName = WINDOWS.equals(os) ? "jpackage.exe" : "jpackage";
        String jpackage = getJavaInstallation().get().getInstallationPath().file("bin/" + executableName).getAsFile().getAbsolutePath();

        // create app image folder
        getExec().exec(e -> {
            e.commandLine(
                    jpackage,
                    "--type",
                    "app-image",
                    "--module",
                    getMainModule().get(),
                    "--resource-dir",
                    resourcesDir.getAsFile().getPath(),
                    "--app-version",
                    getVersion().get(),
                    "--module-path",
                    getModulePath().getAsPath(),
                    "--name",
                    getApplicationName().get(),
                    "--dest",
                    appImageParent.getAsFile().getPath()
            );
            if (getApplicationDescription().isPresent()) {
                e.args("--description", getApplicationDescription().get());
            }
            if (getVendor().isPresent()) {
                e.args("--vendor", getVendor().get());
            }
            if (getCopyright().isPresent()) {
                e.args("--copyright", getCopyright().get());
            }
            for (String javaOption : getJavaOptions().get()) {
                e.args("--java-options", javaOption);
            }
            for (String javaOption : getJlinkOptions().get()) {
                e.args("--jlink-options", javaOption);
            }
            if (!getAddModules().get().isEmpty()) {
                e.args("--add-modules", String.join(",", getAddModules().get()));
            }
            if (getVerbose().get()) {
                e.args("--verbose");
            }
        });

        File appImageFolder = requireNonNull(appImageParent.getAsFile().listFiles())[0];
        File appResourcesFolder;
        if (os.contains("macos")) {
            appResourcesFolder  = new File(appImageFolder, "Contents/app");
        } else if (os.contains("windows")) {
            appResourcesFolder  = new File(appImageFolder, "app");
        } else {
            appResourcesFolder  = new File(appImageFolder, "lib/app");
        }

        // copy additional resource into app-image folder
        getFiles().copy(c -> {
            c.from(getResources());
            c.into(appResourcesFolder);
        });

        // package with additional resources
        getPackageTypes().get().forEach(packageType ->
                getExec().exec(e -> {
                    e.commandLine(
                            jpackage,
                            "--type",
                            packageType,
                            "--app-image",
                            appImageFolder.getPath(),
                            "--dest",
                            getDestination().get().getAsFile().getPath()
                    );
                    for (String option : getOptions().get()) {
                        e.args(option);
                    }
                })
        );

        generateChecksums();
    }

    private void generateChecksums() throws NoSuchAlgorithmException, IOException {
        File destination = getDestination().get().getAsFile();
        List<File> allFiles = Arrays.stream(requireNonNull(destination.listFiles())).filter(File::isFile).collect(Collectors.toList());
        for (File result : allFiles) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(Files.readAllBytes(result.toPath()));
            Files.write(new File(destination, result.getName() + ".sha256").toPath(), bytesToHex(encoded).getBytes());
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
