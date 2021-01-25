/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public class Runtime {
    private OperatingSystem operatingSystem;
    private WebContainer webContainer;
    private JavaVersion javaVersion;

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

    public static Runtime getRuntime(OperatingSystem operatingSystem, WebContainer webContainer, JavaVersion javaVersion) {
        return values().stream()
                .filter(runtime -> runtime.operatingSystem == operatingSystem)
                .filter(runtime -> runtime.webContainer == webContainer)
                .filter(runtime -> runtime.javaVersion == javaVersion)
                .findFirst().orElse(new Runtime(operatingSystem, webContainer, javaVersion));
    }

    public static Runtime getRuntimeFromWebApp(WebAppBase webAppBase) {
        if (StringUtils.startsWithIgnoreCase(webAppBase.linuxFxVersion(), "docker")) {
            return DOCKER;
        }
        return webAppBase.operatingSystem() == com.azure.resourcemanager.appservice.models.OperatingSystem.WINDOWS ?
                getRuntimeFromWindowsWebApp(webAppBase) : getRuntimeFromLinuxWebApp(webAppBase);
    }

    public static Runtime getRuntimeFromLinuxWebApp(WebAppBase webAppBase) {
        if (StringUtils.isEmpty(webAppBase.linuxFxVersion())) {
            return null;
        }
        final String linuxFxVersion = webAppBase.linuxFxVersion().replace("|", " ");
        return getRuntimeFromLinuxFxVersion(linuxFxVersion);
    }

    public static Runtime getRuntimeFromLinuxFxVersion(String linuxFxVersion) {
        final JavaVersion javaVersion = StringUtils.containsIgnoreCase(linuxFxVersion, "java11") ? JavaVersion.JAVA_11 : JavaVersion.JAVA_8;
        final WebContainer webContainer = WebContainer.values().stream()
                .filter(container -> StringUtils.containsIgnoreCase(linuxFxVersion, container.getValue()))
                .findFirst().orElse(null);
        return getRuntime(OperatingSystem.LINUX, webContainer, javaVersion);
    }

    public static Runtime getRuntimeFromWindowsWebApp(WebAppBase webAppBase) {
        if (webAppBase.javaVersion() == null || StringUtils.isAnyEmpty(webAppBase.javaContainer(), webAppBase.javaContainerVersion())) {
            return null;
        }
        final JavaVersion javaVersion = JavaVersion.values().stream()
                .filter(version -> StringUtils.equals(webAppBase.javaVersion().toString(), version.getValue()))
                .findFirst().orElse(null);
        final String javaContainer = String.join(webAppBase.javaContainer(), " ", webAppBase.javaContainerVersion());
        final WebContainer webContainer = StringUtils.equalsIgnoreCase(webAppBase.javaContainer(), "java") ? WebContainer.JAVA_SE :
                WebContainer.values().stream()
                        .filter(container -> StringUtils.equals(javaContainer, container.getValue()))
                        .findFirst().orElse(null);
        return getRuntime(OperatingSystem.WINDOWS, webContainer, javaVersion);
    }

    public RuntimeStack convertToServiceRuntimeStackModel() {
        if (operatingSystem != OperatingSystem.LINUX || webContainer == null || javaVersion == null) {
            return null;
        }
        final String fixedLinuxJavaVersion = this.javaVersion.getValue().startsWith(JavaVersion.JAVA_8.getValue()) ? "8" : "11";
        return RuntimeStack.getAll().stream().filter(runtimeStack -> {
            final String[] versionArray = runtimeStack.version().split("-");
            final String runtimeStackJavaVersion = versionArray[1];
            final String runtimeStackContainer = StringUtils.join(runtimeStack.stack(), " ", versionArray[0]);
            return StringUtils.containsIgnoreCase(runtimeStackJavaVersion, fixedLinuxJavaVersion) &&
                    ((StringUtils.containsIgnoreCase(runtimeStackContainer, "java") && this.webContainer == WebContainer.JAVA_SE) ||
                            StringUtils.equalsIgnoreCase(runtimeStackContainer, this.webContainer.getValue()));
        }).findFirst().orElse(null);
    }

    public com.azure.resourcemanager.appservice.models.WebContainer convertToServiceWebContainerModel() {
        if (operatingSystem != OperatingSystem.WINDOWS || webContainer == null) {
            return null;
        }
        if (webContainer == WebContainer.JAVA_SE) {
            return StringUtils.startsWith(javaVersion.getValue(), JavaVersion.JAVA_8.getValue()) ?
                    com.azure.resourcemanager.appservice.models.WebContainer.JAVA_8 :
                    com.azure.resourcemanager.appservice.models.WebContainer.fromString("java 11");
        }
        return com.azure.resourcemanager.appservice.models.WebContainer.values().stream()
                .filter(container -> StringUtils.equalsIgnoreCase(container.toString(), webContainer.getValue()))
                .findFirst().orElse(null);
    }

    public com.azure.resourcemanager.appservice.models.JavaVersion convertToServiceJavaVersionModel() {
        if (operatingSystem != OperatingSystem.WINDOWS || javaVersion == null) {
            return null;
        }
        return com.azure.resourcemanager.appservice.models.JavaVersion.values().stream()
                .filter(serviceVersion -> StringUtils.equals(serviceVersion.toString(), javaVersion.getValue()))
                .findFirst().orElse(null);
    }

    public static List<Runtime> values() {
        return Arrays.asList(WINDOWS_JAVA8, WINDOWS_JAVA11, WINDOWS_JAVA8_TOMCAT9, WINDOWS_JAVA8_TOMCAT85, WINDOWS_JAVA11_TOMCAT9, WINDOWS_JAVA11_TOMCAT85,
                LINUX_JAVA8, LINUX_JAVA11, LINUX_JAVA8_TOMCAT9, LINUX_JAVA8_TOMCAT85, LINUX_JAVA8_JBOSS72, LINUX_JAVA11_TOMCAT9, LINUX_JAVA11_TOMCAT85);
    }
}
