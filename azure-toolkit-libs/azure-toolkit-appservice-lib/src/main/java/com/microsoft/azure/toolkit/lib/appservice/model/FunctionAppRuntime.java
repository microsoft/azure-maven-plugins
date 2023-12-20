package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

public interface FunctionAppRuntime extends Runtime {
    FunctionAppRuntime DOCKER = FunctionAppDockerRuntime.INSTANCE;
    FunctionAppRuntime DEFAULT = FunctionAppWindowsRuntime.fromJavaVersion(JavaVersion.fromString("17"));

    /**
     * @return java version number, e.g. '1.8'(windows only), '8', '11', '17'
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

    @Override
    default boolean isMinorVersion() {
        if (isDocker()) {
            return false;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        return EXCLUDE_PATTERN.matcher(this.getJavaVersionNumber()).matches();
    }

    @Override
    default JavaVersion getJavaVersion() {
        if (this.isDocker()) {
            return JavaVersion.fromString("docker");
        }
        return JavaVersion.fromString(this.getJavaVersionNumber());
    }
}
