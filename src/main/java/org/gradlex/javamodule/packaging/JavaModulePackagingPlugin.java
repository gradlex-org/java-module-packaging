// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.util.GradleVersion;
import org.gradlex.javamodule.packaging.internal.HostIdentification;

@SuppressWarnings("unused")
public abstract class JavaModulePackagingPlugin implements Plugin<Project> {

    @Inject
    protected abstract JavaToolchainService getJavaToolchains();

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) < 0) {
            throw new RuntimeException("This plugin requires Gradle 7.4+");
        }

        project.getPlugins().apply(JavaPlugin.class);
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceDirectorySet mainResources = sourceSets.getByName("main").getResources();

        JavaModulePackagingExtension javaModulePackaging =
                project.getExtensions().create("javaModulePackaging", JavaModulePackagingExtension.class);
        javaModulePackaging.getApplicationName().convention(project.getName());
        javaModulePackaging.getApplicationVersion().convention(project.provider(() -> (String) project.getVersion()));
        javaModulePackaging.getJpackageResources().convention(project.provider(() -> project.getLayout()
                .getProjectDirectory()
                .dir(mainResources.getSrcDirs().iterator().next().getParent() + "/resourcesPackage")));
        javaModulePackaging.getVerbose().convention(false);
        javaModulePackaging.primaryTarget(HostIdentification.hostTarget(project.getObjects()));
    }
}
