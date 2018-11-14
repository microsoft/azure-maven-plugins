/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

/**
 * Runtime Setting
 */
public class RuntimeSetting {
    private static final String JRE_8 = "jre8";
    private static final String TOMCAT_8_5 = "tomcat 8.5";
    private static final String TOMCAT_9_0 = "tomcat 9.0";
    private static final String WILDFLY_14 = "wildfly 14";

    protected String os;
    protected String javaVersion;
    protected String webContainer;
    protected String image;
    protected String serverId;
    protected String registryUrl;

    public String getOs() {
        return this.os;
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
    }

    public RuntimeStack getLinuxRuntime() throws MojoExecutionException {
        // todo: add unit tests
        if (StringUtils.equalsIgnoreCase(javaVersion, JRE_8)) {
            if (StringUtils.isEmpty(webContainer)) {
                return RuntimeStack.JAVA_8_JRE8;
            }

            switch (webContainer.toLowerCase(Locale.ENGLISH)) {
                case TOMCAT_8_5:
                    return RuntimeStack.TOMCAT_8_5_JRE8;
                case TOMCAT_9_0:
                    return RuntimeStack.TOMCAT_9_0_JRE8;
                case WILDFLY_14:
                    return RuntimeStack.WILDFLY_14_JRE8;
                default:
                    throw new MojoExecutionException(String.format(
                        "Unknown value of <webContainer>. Supported values are %s, %s", TOMCAT_8_5, TOMCAT_9_0));
            }
        }
        throw new MojoExecutionException(String.format(
            "Unknown value of <javaVersion>. Supported values is %s", JRE_8));
    }

    public WebContainer getWebContainer() {
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
}
