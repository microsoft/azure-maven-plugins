package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

public interface Runtime {

    OperatingSystem getOperatingSystem();

    String getDisplayName();

    /**
     * @return java version number, e.g. '8', '11', '17', '17.0.4', '1.8.0_202', '1.8.0_202_ZULU', '1.8'(windows only)
     */
    @Nonnull
    String getJavaVersionNumber();

    default String getJavaVersionUserText() {
        if (this.isDocker()) {
            return "";
        }
        if (StringUtils.equalsAnyIgnoreCase(getJavaVersionNumber(), "1.8", "8")) {
            return "Java 8";
        }
        return String.format("Java %s", getJavaVersionNumber());
    }

    default JavaVersion getJavaVersion() {
        if (this.isDocker()) {
            return JavaVersion.fromString("docker");
        }
        return JavaVersion.fromString(this.getJavaVersionNumber());
    }

    /**
     * @return java major version number, e.g. '7', '8', '11', '17'
     */
    default int getJavaMajorVersionNumber() {
        if (this.isDocker()) {
            return 0;
        }
        final String v = this.getJavaVersionNumber();
        return Integer.parseInt(StringUtils.startsWithIgnoreCase(v, "1.") ? String.valueOf(v.charAt(2)) : v);
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
