/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.util.Arrays;
import java.util.List;

public class V1ConfigurationParser extends ConfigurationParser {
    private static final String TOMCAT_8_5_JRE8 = "tomcat 8.5-jre8";
    private static final String TOMCAT_9_0_JRE8 = "tomcat 9.0-jre8";
    private static final String JRE8 = "jre8";
    private static final List<String> SUPPORTED_LINUX_RUNTIMES = Arrays.asList(TOMCAT_8_5_JRE8, TOMCAT_9_0_JRE8);
    private static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
        "and <containerSettings> should not be set at the same time.";

    public V1ConfigurationParser(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public OperatingSystemEnum getOs() throws MojoExecutionException {
        final String linuxRuntime = mojo.getLinuxRuntime();
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final boolean isContainerSettingEmpty = containerSetting == null || containerSetting.isEmpty();

        // Duplicated runtime are specified
        if (mojo.getJavaVersion() != null ? linuxRuntime != null || !isContainerSettingEmpty :
            linuxRuntime != null && !isContainerSettingEmpty) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }
        if (null != mojo.getJavaVersion()) {
            return OperatingSystemEnum.Windows;
        }
        if (null != linuxRuntime) {
            return OperatingSystemEnum.Linux;
        }
        if (!isContainerSettingEmpty) {
            return OperatingSystemEnum.Docker;
        }
        return null;
    }

    @Override
    protected String getRegion() {
        return mojo.getRegion();
    }

    @Override
    public RuntimeStack getRuntimeStack() throws MojoExecutionException {
        switch (mojo.getLinuxRuntime()) {
            case TOMCAT_8_5_JRE8:
                return RuntimeStack.TOMCAT_8_5_JRE8;
            case TOMCAT_9_0_JRE8:
                return RuntimeStack.TOMCAT_9_0_JRE8;
            case JRE8:
                return RuntimeStack.JAVA_8_JRE8;
            default:
                throw new MojoExecutionException("Unknown value of <linuxRuntime>. " +
                    "The supported values are " + SUPPORTED_LINUX_RUNTIMES.toString());
        }
    }

    @Override
    public String getImage() throws MojoExecutionException {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (containerSetting == null) {
            return null;
        }
        if (StringUtils.isEmpty(containerSetting.getImageName())) {
            throw new MojoExecutionException("Please config the <imageName> of <containerSettings> in pom.xml.");
        }
        return containerSetting.getImageName();
    }

    @Override
    public String getServerId() {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (containerSetting == null) {
            return null;
        }
        return containerSetting.getServerId();
    }

    @Override
    public String getRegistryUrl() {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (containerSetting == null) {
            return null;
        }
        return containerSetting.getRegistryUrl();
    }

    @Override
    public WebContainer getWebContainer() {
        return mojo.getJavaWebContainer();
    }

    @Override
    public JavaVersion getJavaVersion() {
        return mojo.getJavaVersion();
    }

    @Override
    public List<Resource> getResources() {
        // todo
        return null;
    }
}
