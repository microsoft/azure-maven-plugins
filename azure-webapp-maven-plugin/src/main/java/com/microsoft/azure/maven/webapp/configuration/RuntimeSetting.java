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
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


/**
 * Runtime Setting
 */
public class RuntimeSetting {

    /**
     * OS of Web App Below is the list of supported JVM versions:
     * <ul>
     * <li>Windows</li>
     * <li>Linux</li>
     * <li>Docker</li>
     * </ul>
     */
    protected String os;

    /**
     * Java version of Web App Below is the list of supported Java versions:
     * <ul>
     * <li>Java 8</li>
     * <li>Java 11</li>
     * </ul>
     */
    protected String javaVersion;

    /**
     * Web container type and version within Web App. Below is the list of supported
     * web container types(JBoss is only supported on java 8 and linux webapps):
     * <ul>
     * <li>Java SE</li>
     * <li>Tomcat 7.0</li>
     * <li>Tomcat 8.5</li>
     * <li>Tomcat 9.0</li>
     * <li>JBoss EAP 7.2</li>
     * </ul>
     */
    protected String webContainer;

    /**
     * Settings of docker image name within Web App. This only applies to Docker Web
     * App.
     */
    protected String image;

    /**
     * Settings of credentials to access docker image. Use it when you are using
     * private Docker Hub
     */
    protected String serverId;

    /**
     * Settings of specifies your docker image registry URL. Use it when you are
     * using private registry.
     */
    protected String registryUrl;

    public static final String RUNTIME_CONFIG_REFERENCE = "https://aka.ms/maven_webapp_runtime";

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
        final JavaVersion ver = JavaVersionUtils.toAzureSdkJavaVersion(this.javaVersion);
        if (Objects.nonNull(ver)) {
            return ver;
        }
        return (StringUtils.isEmpty(javaVersion) || !checkJavaVersion(javaVersion)) ? null
                : JavaVersion.fromString(javaVersion);
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

    public String getWebContainerRaw() {
        return webContainer;
    }

    public String getJavaVersionRaw() {
        return javaVersion;
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
            if (StringUtils.equalsIgnoreCase(version.toString(), value)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkWebContainer(String value) {
        for (final WebContainer container : WebContainer.values()) {
            if (StringUtils.equalsIgnoreCase(container.toString(), value)) {
                return true;
            }
        }
        return false;
    }
}
