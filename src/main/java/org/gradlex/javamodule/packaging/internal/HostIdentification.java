// SPDX-License-Identifier: Apache-2.0
package org.gradlex.javamodule.packaging.internal;

import java.util.Locale;
import org.gradle.api.model.ObjectFactory;
import org.gradle.nativeplatform.MachineArchitecture;
import org.gradle.nativeplatform.OperatingSystemFamily;
import org.gradlex.javamodule.packaging.model.Target;

public class HostIdentification {

    public static void validateHostSystem(String arch, String os) {
        String hostOs = hostOs();
        String hostArch = hostArch();

        if (!normalizeOs(hostOs).equals(normalizeOs(os))) {
            wrongHostSystemError(hostOs, os);
        }
        if (!normalizeArch(hostArch).equals(normalizeArch(arch))) {
            wrongHostSystemError(hostArch, arch);
        }
    }

    public static Target hostTarget(ObjectFactory objects) {
        Target host = objects.newInstance(Target.class, "host");
        host.getOperatingSystem().convention(hostOs());
        host.getArchitecture().convention(normalizeArch(System.getProperty("os.arch")));
        return host;
    }

    public static boolean isHostTarget(Target target) {
        return target.getOperatingSystem().isPresent()
                && target.getArchitecture().isPresent()
                && target.getOperatingSystem().get().equals(hostOs())
                && target.getArchitecture().get().equals(normalizeArch(hostArch()));
    }

    private static String hostOs() {
        return normalizeOs(System.getProperty("os.name"));
    }

    private static String hostArch() {
        return System.getProperty("os.arch");
    }

    private static String normalizeOs(String name) {
        String os = name.toLowerCase(Locale.ROOT).replace(" ", "");
        if (os.contains("windows")) {
            return OperatingSystemFamily.WINDOWS;
        }
        if (os.contains("macos") || os.contains("darwin") || os.contains("osx")) {
            return OperatingSystemFamily.MACOS;
        }
        return OperatingSystemFamily.LINUX;
    }

    private static String normalizeArch(String name) {
        String arch = name.toLowerCase(Locale.ROOT);
        if (arch.contains("aarch")) {
            return MachineArchitecture.ARM64;
        }
        if (arch.contains("64")) {
            return MachineArchitecture.X86_64;
        }
        return MachineArchitecture.X86;
    }

    private static void wrongHostSystemError(String hostOs, String os) {
        throw new RuntimeException("Running on " + hostOs + "; cannot build for " + os);
    }
}
