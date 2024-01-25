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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.nativeplatform.MachineArchitecture;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradlex.javamodule.packaging.model.Target;
import org.gradlex.javamodule.packaging.tasks.Jpackage;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_GROUP;
import static org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE;
import static org.gradle.nativeplatform.OperatingSystemFamily.LINUX;
import static org.gradle.nativeplatform.OperatingSystemFamily.MACOS;
import static org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE;
import static org.gradle.nativeplatform.OperatingSystemFamily.WINDOWS;

abstract public class JavaModulePackagingExtension {
    private static final Attribute<Boolean> JAVA_MODULE_ATTRIBUTE = Attribute.of("javaModule", Boolean.class);
    private static final String INTERNAL = "internal";

    abstract public Property<String> getApplicationName();
    abstract public Property<String> getApplicationVersion();
    abstract public Property<String> getApplicationDescription();
    abstract public Property<String> getVendor();
    abstract public Property<String> getCopyright();
    abstract public DirectoryProperty getJpackageResources();
    abstract public ConfigurableFileCollection getResources();

    @Inject
    abstract protected JavaToolchainService getJavaToolchains();

    @Inject
    abstract protected ObjectFactory getObjects();

    @Inject
    abstract protected Project getProject();

    public void target(String label, Action<? super Target> action) {
        Target target = getObjects().newInstance(Target.class, label);
        action.execute(target);

        target.getPackageTypes().convention(target.getOperatingSystem().map(os -> {
            switch (os) {
                case WINDOWS:
                    return Arrays.asList("exe", "msi");
                case MACOS:
                    return Arrays.asList("pkg", "dmg");
                case LINUX:
                    return Arrays.asList("rpm", "deb");
            }
            return Collections.emptyList();
        }));

        ConfigurationContainer configurations = getProject().getConfigurations();
        SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);

        sourceSets.all(sourceSet -> {
            // Use first target for target-independent classpaths to make some decision
            maybeConfigureTargetAttributes(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), target);
            maybeConfigureTargetAttributes(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), target);
            // Integration for consistent resolution by 'java-module-dependencies' plugin
            configurations.matching(conf -> "mainRuntimeClasspath".equals(conf.getName())).all(conf -> maybeConfigureTargetAttributes(conf, target));

            Configuration internal = maybeCreateInternalConfiguration();

            configurations.create(target.getLabel() + capitalize(sourceSet.getCompileClasspathConfigurationName()), c -> {
                c.setCanBeConsumed(false);
                c.setVisible(false);
                configureJavaStandardAttributes(c, Usage.JAVA_API);
                maybeConfigureTargetAttributes(c, target);
                c.extendsFrom(
                        configurations.getByName(sourceSet.getImplementationConfigurationName()),
                        configurations.getByName(sourceSet.getCompileOnlyConfigurationName()),
                        internal
                );
            });
            Configuration runtimeClasspath = configurations.create(target.getLabel() + capitalize(sourceSet.getRuntimeClasspathConfigurationName()), c -> {
                c.setCanBeConsumed(false);
                c.setVisible(false);
                configureJavaStandardAttributes(c, Usage.JAVA_RUNTIME);
                maybeConfigureTargetAttributes(c, target);
                c.extendsFrom(
                        configurations.getByName(sourceSet.getImplementationConfigurationName()),
                        configurations.getByName(sourceSet.getRuntimeOnlyConfigurationName()),
                        internal
                );
            });

            if (SourceSet.isMain(sourceSet)) {
                getProject().getPlugins().withType(ApplicationPlugin.class, p -> registerTargetSpecificTasks(target, sourceSet.getJarTaskName(), runtimeClasspath));
            }
        });

    }

    private void configureJavaStandardAttributes(Configuration resolvable, String usage) {
        resolvable.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, getObjects().named(Usage.class, usage));
        resolvable.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE, getObjects().named(Category.class, Category.LIBRARY));
        resolvable.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, getObjects().named(LibraryElements.class, LibraryElements.JAR));
        resolvable.getAttributes().attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
        resolvable.getAttributes().attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling.class, Bundling.EXTERNAL));
        // For integration with 'extra-java-module-info' plugin
        resolvable.getAttributes().attribute(JAVA_MODULE_ATTRIBUTE, true);
    }

    private void maybeConfigureTargetAttributes(Configuration resolvable, Target target) {
        if (!resolvable.getAttributes().contains(OPERATING_SYSTEM_ATTRIBUTE)) {
            resolvable.getAttributes().attributeProvider(OPERATING_SYSTEM_ATTRIBUTE, target.getOperatingSystem().map(name -> getObjects().named(OperatingSystemFamily.class, name)));
            resolvable.getAttributes().attributeProvider(ARCHITECTURE_ATTRIBUTE, target.getArchitecture().map(name -> getObjects().named(MachineArchitecture.class, name)));
        }
    }

    private void registerTargetSpecificTasks(Target target, String applicationJarTask, Configuration runtimeClasspath) {
        TaskContainer tasks = getProject().getTasks();

        JavaPluginExtension java = getProject().getExtensions().getByType(JavaPluginExtension.class);
        JavaApplication application = getProject().getExtensions().getByType(JavaApplication.class);

        TaskProvider<Jpackage> jpackage = tasks.register("jpackage" + capitalize(target.getLabel()), Jpackage.class, t -> {
            t.getJavaInstallation().convention(getJavaToolchains().compilerFor(java.getToolchain()).get().getMetadata());
            t.getOperatingSystem().convention(target.getOperatingSystem());
            t.getArchitecture().convention(target.getArchitecture());
            t.getMainModule().convention(application.getMainModule());
            t.getVersion().convention(getApplicationVersion());
            t.getModulePath().from(tasks.named(applicationJarTask));
            t.getModulePath().from(runtimeClasspath);

            t.getApplicationName().convention(getApplicationName());
            t.getJpackageResources().convention(getJpackageResources().dir(target.getOperatingSystem()));
            t.getApplicationDescription().convention(getApplicationDescription());
            t.getVendor().convention(getVendor());
            t.getCopyright().convention(getCopyright());
            t.getJavaOptions().convention(application.getApplicationDefaultJvmArgs());
            t.getOptions().convention(target.getOptions());
            t.getPackageTypes().convention(target.getPackageTypes());
            t.getResources().from(getResources());

            t.getDestination().convention(getProject().getLayout().getBuildDirectory().dir("packages/" + target.getLabel()));
        });

        tasks.register("run" + capitalize(target.getLabel()), JavaExec.class, t -> {
            t.setGroup(ApplicationPlugin.APPLICATION_GROUP);
            t.setDescription("Run this project as a JVM application on " + target.getLabel());
            t.getJavaLauncher().convention(getJavaToolchains().launcherFor(java.getToolchain()));
            t.getMainModule().convention(application.getMainModule());
            t.setJvmArgs(application.getApplicationDefaultJvmArgs());
            t.classpath(tasks.named("jar"), runtimeClasspath);
        });

        String targetAssembleLifecycle = "assemble" + capitalize(target.getLabel());
        if (!tasks.getNames().contains(targetAssembleLifecycle)) {
            TaskProvider<Task> lifecycleTask = tasks.register(targetAssembleLifecycle, t -> {
                t.setGroup(BUILD_GROUP);
                t.setDescription("Builds this project for " + target.getLabel());
            });
            tasks.named(ASSEMBLE_TASK_NAME, t -> t.dependsOn(lifecycleTask));
        }
        tasks.named(targetAssembleLifecycle, t -> t.dependsOn(jpackage));
    }

    private Configuration maybeCreateInternalConfiguration() {
        Configuration internal = getProject().getConfigurations().findByName(INTERNAL);
        if (internal != null) {
            return internal;
        }
        return getProject().getConfigurations().create(INTERNAL, i -> {
            i.setCanBeResolved(false);
            i.setCanBeConsumed(false);
        });
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
