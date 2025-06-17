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

package org.gradlex.javamodule.packaging;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.util.GradleVersion;
import org.gradlex.javamodule.packaging.internal.HostIdentification;

import javax.inject.Inject;

@SuppressWarnings("unused")
@NonNullApi
public abstract class JavaModulePackagingPlugin implements Plugin<Project> {

    @Inject
    abstract protected JavaToolchainService getJavaToolchains();

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) < 0) {
            throw new RuntimeException("This plugin requires Gradle 7.4+");
        }

        project.getPlugins().apply(JavaPlugin.class);
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceDirectorySet mainResources = sourceSets.getByName("main").getResources();

        JavaModulePackagingExtension javaModulePackaging = project.getExtensions().create("javaModulePackaging", JavaModulePackagingExtension.class);
        javaModulePackaging.getApplicationName().convention(project.getName());
        javaModulePackaging.getApplicationVersion().convention(project.provider(() -> (String) project.getVersion()));
        javaModulePackaging.getJpackageResources().convention(project.provider(() ->
                project.getLayout().getProjectDirectory().dir(mainResources.getSrcDirs().iterator().next().getParent() + "/resourcesPackage")));
        javaModulePackaging.getVerbose().convention(false);
        javaModulePackaging.primaryTarget(HostIdentification.hostTarget(project.getObjects()));
    }
}
