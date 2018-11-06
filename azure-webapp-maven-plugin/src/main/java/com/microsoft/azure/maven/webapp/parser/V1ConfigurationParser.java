/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V1ConfigurationParser extends ConfigurationParser {
    private static final String TOMCAT_8_5_JRE8 = "tomcat 8.5-jre8";
    private static final String TOMCAT_9_0_JRE8 = "tomcat 9.0-jre8";
    private static final String JRE8 = "jre8";
    private static final List<String> SUPPORTED_LINUX_RUNTIMES = Arrays.asList(TOMCAT_8_5_JRE8, TOMCAT_9_0_JRE8, JRE8);
    private static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
        "and <containerSettings> should not be set at the same time.";

    public V1ConfigurationParser(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public OperatingSystemEnum getOs() throws MojoExecutionException {
        final String linuxRuntime = mojo.getLinuxRuntime();
        final JavaVersion javaVersion = mojo.getJavaVersion();
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final boolean isContainerSettingEmpty = containerSetting == null || containerSetting.isEmpty();
        final List<OperatingSystemEnum> osList = new ArrayList<>();

        if (javaVersion != null) {
            osList.add(OperatingSystemEnum.Windows);
        }
        if (linuxRuntime != null) {
            osList.add(OperatingSystemEnum.Linux);
        }
        if (!isContainerSettingEmpty) {
            osList.add(OperatingSystemEnum.Docker);
        }

        if (osList.size() > 1) {
            throw new MojoExecutionException(RUNTIME_CONFIG_CONFLICT);
        }
        return osList.size() > 0 ? osList.get(0) : null;
    }

    @Override
    protected Region getRegion() throws MojoExecutionException {
        if (StringUtils.isEmpty(mojo.getRegion())) {
            return Region.EUROPE_WEST;
        }
        if (Arrays.asList(Region.values()).contains(mojo.getRegion())) {
            throw new MojoExecutionException("The value of <region> is not correct, please correct it in pom.xml.");
        }
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public RuntimeStack getRuntimeStack() throws MojoExecutionException {
        if (mojo.getLinuxRuntime() == null) {
            throw new MojoExecutionException("Please configure the <linuxRuntime> in pom.xml.");
        }
        switch (mojo.getLinuxRuntime()) {
            case TOMCAT_8_5_JRE8:
                return RuntimeStack.TOMCAT_8_5_JRE8;
            case TOMCAT_9_0_JRE8:
                return RuntimeStack.TOMCAT_9_0_JRE8;
            case JRE8:
                return RuntimeStack.JAVA_8_JRE8;
            default:
                throw new MojoExecutionException("The configuration of <linuxRuntime> in pom.xml is not correct. " +
                    "The supported values are " + SUPPORTED_LINUX_RUNTIMES.toString());
        }
    }

    @Override
    public String getImage() throws MojoExecutionException {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (containerSetting == null) {
            throw new MojoExecutionException("Please config the <containerSettings> in pom.xml.");
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
    public WebContainer getWebContainer() throws MojoExecutionException {
        if (mojo.getJavaWebContainer() == null) {
            throw new MojoExecutionException("The configuration of <javaWebContainer> in pom.xml is not correct.");
        }
        return mojo.getJavaWebContainer();
    }

    @Override
    public JavaVersion getJavaVersion() throws MojoExecutionException {
        if (mojo.getJavaVersion() == null) {
            throw new MojoExecutionException("The configuration of <javaVersion> in pom.xml is not correct.");
        }
        return mojo.getJavaVersion();
    }

    @Override
    public List<Resource> getResources() {
        // todo
        return null;
    }
}
