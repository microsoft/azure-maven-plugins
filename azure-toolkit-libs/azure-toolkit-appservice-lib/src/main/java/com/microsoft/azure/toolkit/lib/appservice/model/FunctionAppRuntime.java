package com.microsoft.azure.toolkit.lib.appservice.model;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface FunctionAppRuntime extends Runtime {
    String DEFAULT_JAVA = "Java 17";

    default String getDisplayName() {
        if (this.isDocker()) {
            return "Docker";
        }
        return String.format("%s-%s", this.getOperatingSystem().toString(), this.getJavaVersionUserText());
    }

    @Override
    default boolean isMajorVersion() {
        if (isDocker()) {
            return true;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        return !EXCLUDE_PATTERN.matcher(this.getJavaVersionNumber()).matches();
    }

    static FunctionAppRuntime getDefault() {
        // use static method instead of constant to avoid cyclic dependency
        // https://stackoverflow.com/questions/41016957/java-interface-static-variable-is-not-initialized
        return FunctionAppWindowsRuntime.FUNCTION_JAVA17;
    }

    static List<FunctionAppRuntime> getMajorRuntimes() {
        return Stream.concat(
            Stream.concat(
                FunctionAppLinuxRuntime.getMajorRuntimes().stream(),
                FunctionAppWindowsRuntime.getMajorRuntimes().stream()),
            Stream.of(FunctionAppDockerRuntime.INSTANCE)
        ).collect(Collectors.toList());
    }

    static List<FunctionAppRuntime> getAllRuntimes() {
        return Stream.concat(
            Stream.concat(
                FunctionAppLinuxRuntime.getAllRuntimes().stream(),
                FunctionAppWindowsRuntime.getAllRuntimes().stream()),
            Stream.of(FunctionAppDockerRuntime.INSTANCE)
        ).collect(Collectors.toList());
    }

    static FunctionAppRuntime fromUserText(final String os, String javaVersionUserText) {
        if (StringUtils.equalsIgnoreCase(os, "docker")) {
            return FunctionAppDockerRuntime.INSTANCE;
        }
        if (StringUtils.equalsIgnoreCase(os, "windows")) {
            return FunctionAppWindowsRuntime.fromJavaVersionUserText(javaVersionUserText);
        }
        return FunctionAppLinuxRuntime.fromJavaVersionUserText(javaVersionUserText);
    }

}
