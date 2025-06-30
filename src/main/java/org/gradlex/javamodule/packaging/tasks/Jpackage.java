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
import org.gradle.process.ExecSpec;

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
import static org.gradlex.javamodule.packaging.internal.HostIdentification.validateHostSystem;

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

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract public ConfigurableFileCollection getTargetResources();

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
    abstract public ListProperty<String> getAppImageOptions();

    @Input
    abstract public ListProperty<String> getPackageTypes();

    @Input
    abstract public Property<Boolean> getSingleStepPackaging();

    @Input
    abstract public Property<Boolean> getVerbose();

    @OutputDirectory
    abstract public DirectoryProperty getDestination();

    /**
     * To copy resources before adding them. This allows ressource filtering via Gradle
     * FileCollection and FileTree APIs.
     */
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
        //noinspection ResultOfMethodCallIgnored
        resourcesDir.getAsFile().mkdirs();

        getFiles().copy(c -> {
            c.from(getJpackageResources());
            c.into(resourcesDir);
            c.rename(f -> f.replace("icon", getApplicationName().get()));
        });

        String executableName = WINDOWS.equals(os) ? "jpackage.exe" : "jpackage";
        String jpackage = getJavaInstallation().get().getInstallationPath().file("bin/" + executableName).getAsFile().getAbsolutePath();

        File appContentTmpFolder = getTempDirectory().get().dir("app-content").getAsFile();

        // build 'app-image' target if required (either needed for the next step or explicitly requested)
        if (!getSingleStepPackaging().get() || getPackageTypes().get().contains("app-image")) {
            performAppImageStep(jpackage, resourcesDir);
            File appImageFolder = appImageFolder();
            File appRootFolder;
            if (os.contains("macos")) {
                appRootFolder = new File(appImageFolder, "Contents");
            } else if (os.contains("windows")) {
                appRootFolder = appImageFolder;
            } else {
                appRootFolder = new File(appImageFolder, "lib");
            }
            copyAdditionalRessourcesToImageFolder(appRootFolder);
        }

        if (getSingleStepPackaging().get()) {
            // an isolated folder which is later inserted via '--app-content' parameter
            copyAdditionalRessourcesToImageFolder(appContentTmpFolder);
        }

        // package with additional resources
        getPackageTypes().get().stream().filter(t -> !"app-image".equals(t)).forEach(packageType ->
                getExec().exec(e -> {
                    e.commandLine(
                            jpackage,
                            "--type",
                            packageType,
                            "--app-version",
                            getVersion().get(),
                            "--dest",
                            getDestination().get().getAsFile().getPath()
                    );
                    if (getSingleStepPackaging().get()) {
                        configureJPackageArguments(e, resourcesDir);
                        if (appContentTmpFolder.exists()) {
                            for (File appContent : requireNonNull(appContentTmpFolder.listFiles())) {
                                e.args("--app-content", appContent.getPath());
                            }
                        }
                    } else {
                        e.args("--app-image", appImageFolder().getPath());
                    }
                    for (String option : getOptions().get()) {
                        e.args(option);
                    }
                })
        );

        generateChecksums();
    }

    private File appImageFolder() {
        return Arrays.stream(requireNonNull(getDestination().get().getAsFile().listFiles()))
                .filter(File::isDirectory).findFirst().get();
    }

    private void copyAdditionalRessourcesToImageFolder(File appRootFolder) {
        // copy additional resource into the app-image folder
        getFiles().copy(c -> {
            c.into(appRootFolder);
            c.from(getTargetResources());
            c.from(getResources(), to -> to.into("app")); // 'app' is the folder Java loads resources from at runtime
        });
    }

    private void performAppImageStep(String jpackage, Directory resourcesDir) {
        getExec().exec(e -> {
            e.commandLine(
                    jpackage,
                    "--type",
                    "app-image",
                    "--dest",
                    getDestination().get().getAsFile().getPath()
            );
            configureJPackageArguments(e, resourcesDir);
            for (String option : getAppImageOptions().get()) {
                e.args(option);
            }
        });
    }

    private void configureJPackageArguments(ExecSpec e, Directory resourcesDir) {
        e.args(
                "--module",
                getMainModule().get(),
                "--resource-dir",
                resourcesDir.getAsFile().getPath(),
                "--app-version",
                getVersion().get(),
                "--module-path",
                getModulePath().getAsPath(),
                "--name",
                getApplicationName().get()
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
