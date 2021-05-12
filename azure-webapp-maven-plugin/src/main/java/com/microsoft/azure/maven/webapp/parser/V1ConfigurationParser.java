/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.toolkit.lib.legacy.appservice.OperatingSystemEnum;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class V1ConfigurationParser extends ConfigurationParser {

    private static final String RUNTIME_NOT_EXIST = "The configuration of <linuxRuntime> in pom.xml is not correct. " +
        "Please refer https://aka.ms/maven_webapp_runtime_v1 for more information";

    public V1ConfigurationParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    public OperatingSystemEnum getOs() throws AzureExecutionException {
        validate(validator.validateOs());
        final String linuxRuntime = mojo.getLinuxRuntime();
        final String javaVersion = mojo.getJavaVersion();
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
        return osList.size() > 0 ? osList.get(0) : null;
    }

    @Override
    protected Region getRegion() throws AzureExecutionException {
        validate(validator.validateRegion());
        if (StringUtils.isEmpty(mojo.getRegion())) {
            return Region.EUROPE_WEST;
        }
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public RuntimeStack getRuntimeStack() throws AzureExecutionException {
        validate(validator.validateRuntimeStack());
        final String linuxRuntime = mojo.getLinuxRuntime();
        // JavaSE runtime
        final RuntimeStack javaSERuntimeStack = RuntimeStackUtils.getJavaSERuntimeStack(linuxRuntime);
        if (javaSERuntimeStack != null) {
            return javaSERuntimeStack;
        }
        // Tomcat
        final List<RuntimeStack> runtimeStacks = RuntimeStackUtils.getValidRuntimeStacks();
        for (final RuntimeStack runtimeStack : runtimeStacks) {
            if (runtimeStack.toString().equalsIgnoreCase(mojo.getLinuxRuntime())) {
                return runtimeStack;
            }
        }
        throw new AzureExecutionException(RUNTIME_NOT_EXIST);
    }

    @Override
    public String getImage() throws AzureExecutionException {
        validate(validator.validateImage());
        final ContainerSetting containerSetting = mojo.getContainerSettings();
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
        return "v1";
    }

    @Override
    public WebContainer getWebContainer() throws AzureExecutionException {
        validate(validator.validateWebContainer());
        return mojo.getJavaWebContainer();
    }

    @Override
    public JavaVersion getJavaVersion() throws AzureExecutionException {
        validate(validator.validateJavaVersion());
        return JavaVersion.fromString(mojo.getJavaVersion());
    }

    @Override
    public List<DeploymentResource> getResources() {
        return mojo.getResources();
    }
}
