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
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V1ConfigurationParser extends ConfigurationParser {
    private static final String RUNTIME_CONFIG_CONFLICT = "Conflict settings found. <javaVersion>, <linuxRuntime>" +
        "and <containerSettings> should not be set at the same time.";
    private static final String RUNTIME_NOT_EXIST = "The configuration of <linuxRuntime> in pom.xml is not correct. " +
        "Please refer https://aka.ms/maven_webapp_runtime_v1 for more information";

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
        if (!Arrays.asList(Region.values()).contains(Region.fromName(mojo.getRegion()))) {
            throw new MojoExecutionException("The value of <region> is not correct, please correct it in pom.xml.");
        }
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public RuntimeStack getRuntimeStack() throws MojoExecutionException {
        if (mojo.getLinuxRuntime() == null) {
            throw new MojoExecutionException("Please configure the <linuxRuntime> in pom.xml.");
        }
        final String linuxRuntime = mojo.getLinuxRuntime();
        // JavaSE runtime
        final List<String> validJavaSEStacks = RuntimeStackUtils.getValidJavaVersions();
        if (validJavaSEStacks.contains(linuxRuntime)) {
            return RuntimeStackUtils.getRuntimeStack(linuxRuntime);
        }
        // Tomcat/WildFly
        final List<RuntimeStack> runtimeStacks = RuntimeStackUtils.getValidRuntimeStacks();
        for (final RuntimeStack runtimeStack : runtimeStacks) {
            if (runtimeStack.toString().equalsIgnoreCase(mojo.getLinuxRuntime())) {
                return runtimeStack;
            }
        }
        throw new MojoExecutionException(RUNTIME_NOT_EXIST);
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
    protected String getSchemaVersion() {
        return "V1";
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
        return mojo.getResources();
    }
}
