// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.tasks;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Not worth caching")
public abstract class FatModuleJar extends Jar {

    @Classpath
    public abstract ConfigurableFileCollection getModulePath();

    @Input
    public abstract Property<String> getMainModule();

    @Input
    public abstract Property<String> getMainClass();

    @Classpath
    public abstract ConfigurableFileCollection getLauncherPath();

    @Input
    public abstract Property<String> getLauncherMainClass();

    @Inject
    protected abstract ArchiveOperations getArchives();

    // Should be the public 'FileSystemOperations', but 'copySpec()' was only introduced in 8.5
    @Inject
    protected abstract FileOperations getFiles();

    @Override
    protected void copy() {
        getManifest()
                .attributes(singletonMap("Main-Class", getLauncherMainClass().get()));

        CopySpecInternal extendedSpec = (CopySpecInternal) getFiles().copySpec();
        extendedSpec.with(getRootSpec());

        File applicationProperties = writeApplicationProperties();
        extendedSpec.from(applicationProperties);

        extendedSpec.from(getLauncherPath().getFiles().stream()
                .map(jar -> getArchives().zipTree(jar).matching(f -> f.exclude("META-INF/MANIFEST.MF")))
                .collect(Collectors.toList()));

        extendedSpec.into("modulepath", pathFolder -> {
            for (File jar : getModulePath()) {
                pathFolder.into(
                        nameWithoutExtension(jar),
                        moduleFolder -> moduleFolder.from(getArchives().zipTree(jar)));
            }
        });

        // based on 'super()'
        WorkResult didWork = createCopyActionExecuter().execute(extendedSpec, createCopyAction());
        setDidWork(didWork.getDidWork());
    }

    private File writeApplicationProperties() {
        File applicationProperties = new File(getTemporaryDir(), "application.properties");
        try {
            Files.createDirectories(applicationProperties.toPath().getParent());
            Files.write(
                    applicationProperties.toPath(),
                    String.format(
                                    "mainModule=%s\nmainClass=%s",
                                    getMainModule().get(), getMainClass().get())
                            .getBytes(UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return applicationProperties;
    }

    private String nameWithoutExtension(File file) {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }
}
