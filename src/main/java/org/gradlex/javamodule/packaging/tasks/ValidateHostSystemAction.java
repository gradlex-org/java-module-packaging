// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.tasks;

import static org.gradlex.javamodule.packaging.internal.HostIdentification.validateHostSystem;

import java.util.Map;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ValidateHostSystemAction implements Action<Task> {
    @Override
    public void execute(Task task) {
        Map<String, Object> inputs = task.getInputs().getProperties();
        validateHostSystem((String) inputs.get("architecture"), (String) inputs.get("operatingSystem"));
    }
}
