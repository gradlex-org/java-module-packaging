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
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.NonNullApi;
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
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.nativeplatform.MachineArchitecture;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradle.testing.base.TestSuite;
import org.gradlex.javamodule.packaging.model.Target;
import org.gradlex.javamodule.packaging.tasks.Jpackage;
import org.gradlex.javamodule.packaging.tasks.ValidateHostSystemAction;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_GROUP;
import static org.gradle.nativeplatform.MachineArchitecture.ARCHITECTURE_ATTRIBUTE;
import static org.gradle.nativeplatform.OperatingSystemFamily.LINUX;
import static org.gradle.nativeplatform.OperatingSystemFamily.MACOS;
import static org.gradle.nativeplatform.OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE;
import static org.gradle.nativeplatform.OperatingSystemFamily.WINDOWS;

@NonNullApi
abstract public class JavaModulePackagingExtension {
    private static final Attribute<Boolean> JAVA_MODULE_ATTRIBUTE = Attribute.of("javaModule", Boolean.class);
    private static final String INTERNAL = "internal";

    abstract public Property<String> getApplicationName();
    abstract public Property<String> getApplicationVersion();
    abstract public Property<String> getApplicationDescription();
    abstract public Property<String> getVendor();
    abstract public Property<String> getCopyright();
    abstract public ListProperty<String> getJlinkOptions();
    abstract public ListProperty<String> getAddModules();
    abstract public DirectoryProperty getJpackageResources();
    abstract public ConfigurableFileCollection getResources();
    abstract public Property<Boolean> getVerbose();

    private final NamedDomainObjectContainer<Target> targets = getObjects().domainObjectContainer(Target.class);

    @Inject
    abstract protected JavaToolchainService getJavaToolchains();

    @Inject
    abstract protected ObjectFactory getObjects();

    @Inject
    abstract protected Project getProject();


    /**
     * Retrieve the target with the given 'label'. If the target does not yet exist, it will be created.
     */
    @SuppressWarnings("unused")
    public Target target(String label) {
        return target(label, target -> {});
    }

    /**
     * Register or update a target with the given 'label'. The 'label' uniquely identifies the target.
     * It is used for task names and can be chosen freely.
     * Details of the target are configured in the {@link Target} configuration action.
     */
    public Target target(String label, Action<? super Target> action) {
        Target target;
        if (targets.getNames().contains(label)) {
            target = targets.getByName(label);
        } else {
            target = targets.create(label, this::newTarget);
        }

        action.execute(target);
        return target;
    }

    /**
     * Configure all targets for the given OS.
     */
    @SuppressWarnings("unused")
    public void targetsWithOs(String operatingSystem, Action<? super Target> action) {
        NamedDomainObjectSet<Target> matches = targets.matching(t ->
                t.getOperatingSystem().isPresent() && t.getOperatingSystem().get().equals(operatingSystem));
        matches.all(action);
    }

    /**
     * Set a 'primary target'. Standard Gradle tasks that are not bound to a specific target – like 'assemble' – use
     * this 'primary target'.
     */
    @SuppressWarnings("unused")
    public Target primaryTarget(Target target) {
        SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
        ConfigurationContainer configurations = getProject().getConfigurations();

        sourceSets.all(sourceSet -> {
            // Use this target for target-independent classpaths to make some decision
            configureTargetAttributes(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), target);
            configureTargetAttributes(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), target);
            // Integration for consistent resolution by 'java-module-dependencies' plugin
            configurations.matching(conf -> "mainRuntimeClasspath".equals(conf.getName())).all(conf -> configureTargetAttributes(conf, target));
        });

        return target;
    }

    /**
     * Set a test suite to be 'multi-target'. This registers an additional 'test' task for each target.
     */
    @SuppressWarnings({"unused", "UnstableApiUsage"})
    public TestSuite multiTargetTestSuite(TestSuite testSuite) {
        if (!(testSuite instanceof JvmTestSuite)) {
            return testSuite;
        }

        JvmTestSuite suite = (JvmTestSuite) testSuite;
        targets.all(target -> suite.getTargets().register(testSuite.getName() + capitalize(target.getName()), testTarget -> testTarget.getTestTask().configure(task -> {
            task.getInputs().property("operatingSystem", target.getOperatingSystem());
            task.getInputs().property("architecture", target.getArchitecture());

            ConfigurationContainer configurations = getProject().getConfigurations();
            task.setClasspath(configurations.getByName(target.getName() + capitalize(suite.getSources().getRuntimeClasspathConfigurationName())).plus(
                    getObjects().fileCollection().from(getProject().getTasks().named(suite.getSources().getJarTaskName())))
            );
            task.doFirst(new ValidateHostSystemAction());
        })));

        return testSuite;
    }

    private void newTarget(Target target) {
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
            Configuration internal = maybeCreateInternalConfiguration();
            configurations.create(target.getName() + capitalize(sourceSet.getCompileClasspathConfigurationName()), c -> {
                c.setCanBeConsumed(false);
                c.setVisible(false);
                configureJavaStandardAttributes(c, Usage.JAVA_API);
                configureTargetAttributes(c, target);
                c.extendsFrom(
                        configurations.getByName(sourceSet.getImplementationConfigurationName()),
                        configurations.getByName(sourceSet.getCompileOnlyConfigurationName()),
                        internal
                );
            });
            Configuration runtimeClasspath = configurations.create(target.getName() + capitalize(sourceSet.getRuntimeClasspathConfigurationName()), c -> {
                c.setCanBeConsumed(false);
                c.setVisible(false);
                configureJavaStandardAttributes(c, Usage.JAVA_RUNTIME);
                configureTargetAttributes(c, target);
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

    private void configureTargetAttributes(Configuration resolvable, Target target) {
        resolvable.getAttributes().attributeProvider(OPERATING_SYSTEM_ATTRIBUTE, target.getOperatingSystem().map(name -> getObjects().named(OperatingSystemFamily.class, name)));
        resolvable.getAttributes().attributeProvider(ARCHITECTURE_ATTRIBUTE, target.getArchitecture().map(name -> getObjects().named(MachineArchitecture.class, name)));
    }

    private void registerTargetSpecificTasks(Target target, String applicationJarTask, Configuration runtimeClasspath) {
        TaskContainer tasks = getProject().getTasks();

        JavaPluginExtension java = getProject().getExtensions().getByType(JavaPluginExtension.class);
        JavaApplication application = getProject().getExtensions().getByType(JavaApplication.class);

        TaskProvider<Jpackage> jpackage = tasks.register("jpackage" + capitalize(target.getName()), Jpackage.class, t -> {
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
            t.getJlinkOptions().convention(getJlinkOptions());
            t.getAddModules().convention(getAddModules());
            t.getOptions().convention(target.getOptions());
            t.getPackageTypes().convention(target.getPackageTypes());
            t.getResources().from(getResources());
            t.getTargetResources().from(target.getTargetResources());
            t.getVerbose().convention(getVerbose());

            t.getDestination().convention(getProject().getLayout().getBuildDirectory().dir("packages/" + target.getName()));
            t.getTempDirectory().convention(getProject().getLayout().getBuildDirectory().dir("tmp/jpackage/" + target.getName()));
        });

        tasks.register("run" + capitalize(target.getName()), JavaExec.class, t -> {
            t.setGroup(ApplicationPlugin.APPLICATION_GROUP);
            t.setDescription("Run this project as a JVM application on " + target.getName());
            t.getJavaLauncher().convention(getJavaToolchains().launcherFor(java.getToolchain()));
            t.getMainModule().convention(application.getMainModule());
            t.getMainClass().convention(application.getMainClass());
            t.setJvmArgs(application.getApplicationDefaultJvmArgs());
            t.classpath(tasks.named("jar"), runtimeClasspath);
        });

        String targetAssembleLifecycle = "assemble" + capitalize(target.getName());
        if (!tasks.getNames().contains(targetAssembleLifecycle)) {
            tasks.register(targetAssembleLifecycle, t -> {
                t.setGroup(BUILD_GROUP);
                t.setDescription("Builds this project for " + target.getName());
            });
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
