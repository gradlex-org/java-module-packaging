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

package org.gradlex.javamodule.packaging.internal;

public class Validator {
    public static void validateHostSystem(String arch, String os) {
        String hostOs = System.getProperty("os.name").replace(" ", "").toLowerCase();
        String hostArch = System.getProperty("os.arch");

        if (os.contains("macos")) {
            if (!hostOs.contains(os)) {
                wrongHostSystemError(hostOs, os);
            }
        } else if (os.contains("windows")) {
            if (!hostOs.contains(os)) {
                wrongHostSystemError(hostOs, os);
            }
        } else {
            if (hostOs.contains("windows") || hostOs.contains("macos")) {
                wrongHostSystemError(hostOs, os);
            }
        }

        if (arch.contains("64") && !hostArch.contains("64")) {
            wrongHostSystemError(hostArch, arch);
        }
        if (arch.contains("aarch") && !hostArch.contains("aarch")) {
            wrongHostSystemError(hostArch, arch);
        }
        if (!arch.contains("aarch") && hostArch.contains("aarch")) {
            wrongHostSystemError(hostArch, arch);
        }
    }

    public static void wrongHostSystemError(String hostOs, String os) {
        throw new RuntimeException("Running on " + hostOs + "; cannot build for " + os);
    }
}
