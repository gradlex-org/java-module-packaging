// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.model;

import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Target {

    private final String name;

    public abstract Property<String> getOperatingSystem();

    public abstract Property<String> getArchitecture();

    public abstract ListProperty<String> getPackageTypes();

    public abstract ListProperty<String> getOptions();

    public abstract ListProperty<String> getAppImageOptions();

    public abstract ConfigurableFileCollection getTargetResources();

    public abstract Property<Boolean> getSingleStepPackaging();

    /**
     * An optional step to run on the created app image, after all resources have been copied into it and before
     * the OS-specific packages are built from that image. Not supported together with 'singleStepPackaging'.
     */
    public abstract Property<Action<Directory>> getPostAppImageStep();

    @Inject
    public Target(String name) {
        this.name = name;
        getSingleStepPackaging().convention(false);
    }

    public String getName() {
        return name;
    }
}
