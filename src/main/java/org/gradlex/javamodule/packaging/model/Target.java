// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.model;

import javax.inject.Inject;
import org.gradle.api.file.ConfigurableFileCollection;
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

    @Inject
    public Target(String name) {
        this.name = name;
        getSingleStepPackaging().convention(false);
    }

    public String getName() {
        return name;
    }
}
