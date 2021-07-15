/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Runtime {
    private static final Pattern LINUX_FX_VERSION_PATTERN = Pattern.compile("(.+)\\|((.*)-)?(.+)");

    // Web App
    public static final Runtime WINDOWS_JAVA8 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_SE, JavaVersion.JAVA_8);
    public static final Runtime WINDOWS_JAVA11 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_SE, JavaVersion.JAVA_11);
    public static final Runtime WINDOWS_JAVA8_TOMCAT9 = new Runtime(OperatingSystem.WINDOWS, WebContainer.TOMCAT_9, JavaVersion.JAVA_8);
    public static final Runtime WINDOWS_JAVA8_TOMCAT85 = new Runtime(OperatingSystem.WINDOWS, WebContainer.TOMCAT_85, JavaVersion.JAVA_8);
    public static final Runtime WINDOWS_JAVA11_TOMCAT9 = new Runtime(OperatingSystem.WINDOWS, WebContainer.TOMCAT_9, JavaVersion.JAVA_11);
    public static final Runtime WINDOWS_JAVA11_TOMCAT85 = new Runtime(OperatingSystem.WINDOWS, WebContainer.TOMCAT_85, JavaVersion.JAVA_11);
    public static final Runtime LINUX_JAVA8 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_SE, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA11 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_SE, JavaVersion.JAVA_11);
    public static final Runtime LINUX_JAVA8_TOMCAT9 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_9, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA8_TOMCAT85 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_85, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA8_JBOSS7 = new Runtime(OperatingSystem.LINUX, WebContainer.JBOSS_7, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA11_JBOSS7 = new Runtime(OperatingSystem.LINUX, WebContainer.JBOSS_7, JavaVersion.JAVA_11);
    public static final Runtime LINUX_JAVA8_JBOSS72 = new Runtime(OperatingSystem.LINUX, WebContainer.JBOSS_72, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA11_TOMCAT9 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_9, JavaVersion.JAVA_11);
    public static final Runtime LINUX_JAVA11_TOMCAT85 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_85, JavaVersion.JAVA_11);
    // Function
    public static final Runtime FUNCTION_WINDOWS_JAVA8 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF, JavaVersion.JAVA_8);
    public static final Runtime FUNCTION_WINDOWS_JAVA11 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF, JavaVersion.JAVA_11);
    public static final Runtime FUNCTION_LINUX_JAVA8 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.JAVA_8);
    public static final Runtime FUNCTION_LINUX_JAVA11 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.JAVA_11);
    // Docker
    public static final Runtime DOCKER = new Runtime(OperatingSystem.DOCKER, null, null);

    public static final List<Runtime> WEBAPP_RUNTIME = Collections.unmodifiableList(Arrays.asList(WINDOWS_JAVA8, WINDOWS_JAVA11, WINDOWS_JAVA8_TOMCAT9,
            WINDOWS_JAVA8_TOMCAT85, WINDOWS_JAVA11_TOMCAT9, WINDOWS_JAVA11_TOMCAT85, LINUX_JAVA8, LINUX_JAVA11, LINUX_JAVA8_TOMCAT9, LINUX_JAVA8_TOMCAT85,
            LINUX_JAVA8_JBOSS72, LINUX_JAVA11_JBOSS7, LINUX_JAVA8_JBOSS7, LINUX_JAVA11_TOMCAT9, LINUX_JAVA11_TOMCAT85));
    public static final List<Runtime> FUNCTION_APP_RUNTIME = Collections.unmodifiableList(Arrays.asList(FUNCTION_LINUX_JAVA8, FUNCTION_LINUX_JAVA11,
            FUNCTION_WINDOWS_JAVA8, FUNCTION_WINDOWS_JAVA11));
    private static final List<Runtime> values =
            Collections.unmodifiableList(new ArrayList<>(new HashSet<>(ListUtils.union(WEBAPP_RUNTIME, FUNCTION_APP_RUNTIME))));

    private OperatingSystem operatingSystem;
    private WebContainer webContainer;
    private JavaVersion javaVersion;

    public static Runtime getRuntime(OperatingSystem operatingSystem, WebContainer webContainer, JavaVersion javaVersion) {
        final Runtime standardRuntime = values().stream()
            .filter(runtime -> Objects.equals(runtime.operatingSystem, operatingSystem))
            .filter(runtime -> Objects.equals(runtime.webContainer, webContainer))
            .filter(runtime -> Objects.equals(runtime.javaVersion, javaVersion))
            .findFirst().orElse(null);
        if (standardRuntime != null) {
            return standardRuntime;
        }
        return new Runtime(operatingSystem, webContainer, javaVersion);
    }

    public static Runtime getRuntimeFromLinuxFxVersion(String linuxFxVersion) {
        final Matcher matcher = LINUX_FX_VERSION_PATTERN.matcher(linuxFxVersion);
        if (!matcher.matches()) {
            return getRuntime(OperatingSystem.LINUX, WebContainer.JAVA_OFF, JavaVersion.OFF);
        }
        final String javaVersion = matcher.group(4);
        final String webContainer = String.format("%s %s", matcher.group(1), matcher.group(3)).trim();
        return getRuntime(OperatingSystem.LINUX, WebContainer.fromString(webContainer), JavaVersion.fromString(javaVersion));
    }

    public static List<Runtime> values() {
        return values;
    }

    public boolean isWindows() {
        return Objects.equals(operatingSystem, OperatingSystem.WINDOWS);
    }

    public boolean isLinux() {
        return Objects.equals(operatingSystem, OperatingSystem.LINUX);
    }

    public boolean isDocker() {
        return Objects.equals(operatingSystem, OperatingSystem.DOCKER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Runtime)) {
            return false;
        }
        final Runtime runtime = (Runtime) o;
        return operatingSystem == runtime.operatingSystem && Objects.equals(webContainer, runtime.webContainer) &&
            Objects.equals(javaVersion, runtime.javaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operatingSystem, webContainer, javaVersion);
    }

    @Override
    public String toString() {
        if (isDocker()) {
            return "Docker";
        }

        return Stream.of(getOperatingSystem(), getJavaVersion(), getWebContainer())
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.joining("|"));
    }
}
