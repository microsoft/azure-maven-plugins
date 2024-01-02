package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;

public interface Runtime {

    OperatingSystem getOperatingSystem();

    String getDisplayName();

    /**
     * @return java version number, e.g. '1.8'(no '8'), '11', '17', '17.0.4', '1.8.0_202', '1.8.0_202_ZULU'
     */
    @Nonnull
    String getJavaVersionNumber();

    /**
     * @return java version user text, e.g. 'Java 8'(no 'Java 8'), 'Java 11', 'Java 17', 'Java 17.0.4', 'Java 1.8.0_202', 'Java 1.8.0_202_ZULU'
     */
    default String getJavaVersionUserText() {
        if (this.isDocker()) {
            return "";
        }
        if (StringUtils.equalsAnyIgnoreCase(getJavaVersionNumber(), "1.8", "8")) {
            return "Java 8";
        }
        return String.format("Java %s", getJavaVersionNumber());
    }

    /**
     * @return java version, e.g. '1.8'(no '8'), '11', '17', 'docker'
     */
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
        return Runtime.getJavaMajorVersionNumber(v);
    }

    default boolean isDeprecated() {
        return false;
    }

    default boolean isHidden() {
        return false;
    }

    default boolean isEarlyAccess() {
        return false;
    }

    default boolean isAutoUpdate() {
        return false;
    }

    default boolean isPreview() {
        return false;
    }

    @Nullable
    default OffsetDateTime getEndOfLifeDate(){
        return null;
    }

    default boolean isMajorVersion() {
        return true;
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
        final String[] parts = v.split("\\.", 3);
        return Integer.parseInt(StringUtils.startsWithIgnoreCase(v, "1.") ? parts[1] : parts[0]);
    }

    /**
     * 8u202 => 1.8.0_202, 7 => 1.7, 8 => 1.8, 11 => 11, 17 => 17, 17.0.4 => 17.0.4, java 8 => 1.8
     */
    static String extractAndFormalizeJavaVersionNumber(String javaVersion) {
        javaVersion = StringUtils.replaceIgnoreCase(javaVersion, "java", "").trim();
        javaVersion = StringUtils.equalsAny(javaVersion, "8", "7") ? "1." + javaVersion : javaVersion;
        if (javaVersion.contains("u")) {
            String[] parts = javaVersion.split("u", 2);
            return String.format("1.%s.0_%s", parts[0], parts[1]);
        }
        return javaVersion;
    }
}
