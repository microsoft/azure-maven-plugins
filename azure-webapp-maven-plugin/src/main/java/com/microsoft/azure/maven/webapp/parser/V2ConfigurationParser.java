/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;

import org.apache.maven.model.Resource;

import java.util.List;
import java.util.Locale;

@Deprecated
public class V2ConfigurationParser extends ConfigurationParser {

    public V2ConfigurationParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    protected OperatingSystemEnum getOs() throws AzureExecutionException {
        validate(validator.validateOs());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        final String os = runtime.getOs();
        if (runtime.isEmpty()) {
            return null;
        }
        switch (os.toLowerCase(Locale.ENGLISH)) {
            case "windows":
                return OperatingSystemEnum.Windows;
            case "linux":
                return OperatingSystemEnum.Linux;
            case "docker":
                return OperatingSystemEnum.Docker;
            default:
                return null;
        }
    }

    @Override
    protected Region getRegion() throws AzureExecutionException {
        validate(validator.validateRegion());
        final String region = mojo.getRegion();
        return Region.fromName(region);
    }

    @Override
    protected RuntimeStack getRuntimeStack() throws AzureExecutionException {
        validate(validator.validateRuntimeStack());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        return runtime.getLinuxRuntime();
    }

    @Override
    protected String getImage() throws AzureExecutionException {
        validate(validator.validateImage());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getImage();
    }

    @Override
    protected String getServerId() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getServerId();
    }

    @Override
    protected String getRegistryUrl() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getRegistryUrl();
    }

    @Override
    protected String getSchemaVersion() {
        return "v2";
    }

    @Override
    protected WebContainer getWebContainer() throws AzureExecutionException {
        validate(validator.validateWebContainer());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getWebContainer();
    }

    @Override
    protected JavaVersion getJavaVersion() throws AzureExecutionException {
        validate(validator.validateJavaVersion());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getJavaVersion();
    }

    @Override
    protected List<Resource> getResources() {
        final Deployment deployment = mojo.getDeployment();
        return deployment == null ? null : deployment.getResources();
    }
}
