package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import org.apache.commons.lang3.StringUtils;

public interface Runtime {

    OperatingSystem getOperatingSystem();

    default JavaVersion getJavaVersion() {
        return JavaVersion.fromString("<null>");
    }

    default boolean isDeprecatedOrHidden() {
        return false;
    }

    default boolean isMinorVersion() {
        return false;
    }

    default boolean isWindows() {
        return this.getOperatingSystem() == OperatingSystem.WINDOWS;
    }

    default boolean isLinux() {
        return this.getOperatingSystem() == OperatingSystem.LINUX;
    }

    default boolean isDocker() {
        return this.getOperatingSystem() == OperatingSystem.DOCKER;
    }

    static int getJavaMajorVersionNumber(String javaVersionUserText) {
        final String v = StringUtils.replaceIgnoreCase(javaVersionUserText, "java", "").trim();
        return Integer.parseInt(StringUtils.startsWithIgnoreCase(v, "1.") ? String.valueOf(v.charAt(2)) : v);
    }
}
