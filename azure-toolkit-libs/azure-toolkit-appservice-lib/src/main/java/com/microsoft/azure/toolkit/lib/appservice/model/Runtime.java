/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class Runtime {

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
    public static final Runtime LINUX_JAVA8_JBOSS72 = new Runtime(OperatingSystem.LINUX, WebContainer.JBOSS_72, JavaVersion.JAVA_8);
    public static final Runtime LINUX_JAVA11_TOMCAT9 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_9, JavaVersion.JAVA_11);
    public static final Runtime LINUX_JAVA11_TOMCAT85 = new Runtime(OperatingSystem.LINUX, WebContainer.TOMCAT_85, JavaVersion.JAVA_11);
    public static final Runtime DOCKER = new Runtime(OperatingSystem.DOCKER, null, null);

    private static final List<Runtime> values = Collections.unmodifiableList(Arrays.asList(WINDOWS_JAVA8, WINDOWS_JAVA11, WINDOWS_JAVA8_TOMCAT9,
        WINDOWS_JAVA8_TOMCAT85, WINDOWS_JAVA11_TOMCAT9, WINDOWS_JAVA11_TOMCAT85, LINUX_JAVA8, LINUX_JAVA11, LINUX_JAVA8_TOMCAT9, LINUX_JAVA8_TOMCAT85,
        LINUX_JAVA8_JBOSS72, LINUX_JAVA11_TOMCAT9, LINUX_JAVA11_TOMCAT85));

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
        final String[] runtimeDetails = linuxFxVersion.split("-");
        if (runtimeDetails.length != 2) {
            return null;
        }
        final String javaVersionRaw = runtimeDetails[1];
        final String webContainerRaw = runtimeDetails[0];
        final JavaVersion javaVersion = StringUtils.containsIgnoreCase(javaVersionRaw, "java11") ? JavaVersion.JAVA_11 : JavaVersion.JAVA_8;
        final WebContainer webContainer = WebContainer.fromString(webContainerRaw);
        return getRuntime(OperatingSystem.LINUX, webContainer, javaVersion);
    }

    public static List<Runtime> values() {
        return values;
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
}
