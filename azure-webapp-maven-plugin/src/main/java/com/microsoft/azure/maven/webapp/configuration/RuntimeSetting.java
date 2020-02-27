/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.common.Utils;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;

import org.apache.commons.lang3.StringUtils;

import static com.microsoft.azure.maven.webapp.ConfigMojo.JAVA_11_STRING;

/**
 * Runtime Setting
 */
public class RuntimeSetting {

    protected String os;
    protected String javaVersion;
    protected String webContainer;
    protected String image;
    protected String serverId;
    protected String registryUrl;

    public static final String RUNTIME_CONFIG_REFERENCE = "https://aka.ms/maven_webapp_runtime";

    static {
        WebContainer.fromString(JAVA_11_STRING); // Add Java 11 Enum as Fluent SDK had not added it yet
    }

    public String getOs() {
        return this.os;
    }

    public OperatingSystemEnum getOsEnum() {
        try {
            return Utils.parseOperationSystem(this.os);
        } catch (AzureExecutionException e) {
            return null;
        }
    }

    public JavaVersion getJavaVersion() {
        return (StringUtils.isEmpty(javaVersion) || !checkJavaVersion(javaVersion)) ?
                null : JavaVersion.fromString(javaVersion);
    }

    public RuntimeStack getLinuxRuntime() {
        // todo: add unit tests
        return RuntimeStackUtils.getRuntimeStack(javaVersion, webContainer);
    }

    public WebContainer getWebContainer() {
        if (!checkWebContainer(webContainer)) {
            return null;
        }
        if (StringUtils.isEmpty(webContainer)) {
            return WebContainer.TOMCAT_8_5_NEWEST;
        }
        return WebContainer.fromString(webContainer);
    }

    public String getImage() {
        return this.image;
    }

    public String getServerId() {
        return this.serverId;
    }

    public String getRegistryUrl() {
        return this.registryUrl;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(this.os) && StringUtils.isEmpty(this.javaVersion) &&
            StringUtils.isEmpty(this.webContainer) && StringUtils.isEmpty(image) &&
            StringUtils.isEmpty(this.serverId) && StringUtils.isEmpty(this.registryUrl);
    }

    protected boolean checkJavaVersion(String value) {
        for (final JavaVersion version : JavaVersion.values()) {
            if (version.toString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkWebContainer(String value) {
        for (final WebContainer container : WebContainer.values()) {
            if (container.toString().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
