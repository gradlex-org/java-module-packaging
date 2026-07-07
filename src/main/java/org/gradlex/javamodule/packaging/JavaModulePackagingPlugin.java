// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging;

import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.Category.LIBRARY;
import static org.gradle.api.attributes.Usage.JAVA_RUNTIME;
import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GradleVersion;
import org.gradlex.javamodule.packaging.internal.HostIdentification;
import org.gradlex.javamodule.packaging.model.Target;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class JavaModulePackagingPlugin implements Plugin<Project> {

    private static final String DEFAULT_FATJAR_LAUNCHER = "build.jenesis:build.jenesis.launcher:0.3.0";

    @Override
    public void apply(Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) < 0) {
            throw new RuntimeException("This plugin requires Gradle 7.4+");
        }

        project.getPlugins().apply(JavaPlugin.class);
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceDirectorySet mainResources = sourceSets.getByName("main").getResources();

        JavaModulePackagingExtension javaModulePackaging =
                project.getExtensions().create("javaModulePackaging", JavaModulePackagingExtension.class, project);
        javaModulePackaging.getApplicationName().convention(project.getName());
        javaModulePackaging.getApplicationVersion().convention(project.provider(() -> (String) project.getVersion()));
        javaModulePackaging.getJpackageResources().convention(project.provider(() -> project.getLayout()
                .getProjectDirectory()
                .dir(mainResources.getSrcDirs().iterator().next().getParent() + "/resourcesPackage")));
        javaModulePackaging.getVerbose().convention(false);

        Target hostTarget = HostIdentification.hostTarget(project.getObjects());
        javaModulePackaging.primaryTarget(hostTarget);
        project.afterEvaluate(__ -> javaModulePackaging.maybeAddSingleDefaultTarget(hostTarget));

        registerFatModuleJarLauncherScope(project);
    }

    private void registerFatModuleJarLauncherScope(Project project) {
        ObjectFactory objects = project.getObjects();
        ConfigurationContainer configurations = project.getConfigurations();
        DependencyHandler dependencies = project.getDependencies();

        Provider<Configuration> fatModuleJarLauncher = configurations.register("fatModuleJarLauncher", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(false);
            c.withDependencies(deps -> {
                if (deps.isEmpty()) {
                    deps.add(dependencies.create(DEFAULT_FATJAR_LAUNCHER));
                }
            });
        });

        configurations.register("fatModuleJarLauncherPath", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.extendsFrom(fatModuleJarLauncher.get());
            c.getAttributes().attribute(USAGE_ATTRIBUTE, objects.named(Usage.class, JAVA_RUNTIME));
            c.getAttributes().attribute(CATEGORY_ATTRIBUTE, objects.named(Category.class, LIBRARY));
        });
    }
}
