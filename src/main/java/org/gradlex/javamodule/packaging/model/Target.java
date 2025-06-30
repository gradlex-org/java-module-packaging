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

package org.gradlex.javamodule.packaging.model;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

abstract public class Target {

    private final String name;

    abstract public Property<String> getOperatingSystem();
    abstract public Property<String> getArchitecture();

    abstract public ListProperty<String> getPackageTypes();
    abstract public ListProperty<String> getOptions();
    abstract public ListProperty<String> getAppImageOptions();

    abstract public ConfigurableFileCollection getTargetResources();

    abstract public Property<Boolean> getSingleStepPackaging();

    @Inject
    public Target(String name) {
        this.name = name;
        getSingleStepPackaging().convention(false);
    }

    public String getName() {
        return name;
    }
}
