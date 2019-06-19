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
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.validator.ConfigurationValidator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;
import java.util.Locale;

public class V2ConfigurationParser extends ConfigurationParser {

    public V2ConfigurationParser(AbstractWebAppMojo mojo, ConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    protected OperatingSystemEnum getOs() throws MojoExecutionException {
        validate(validator.validateOs());
        final RuntimeSetting runtime = mojo.getRuntime();
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
    protected Region getRegion() throws MojoExecutionException {
        validate(validator.validateRegion());
        final String region = mojo.getRegion();
        return Region.fromName(region);
    }

    @Override
    protected RuntimeStack getRuntimeStack() throws MojoExecutionException {
        validate(validator.validateRuntimeStack());
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null || runtime.isEmpty()) {
            return null;
        }
        return runtime.getLinuxRuntime();
    }

    @Override
    protected String getImage() throws MojoExecutionException {
        validate(validator.validateImage());
        final RuntimeSetting runtime = mojo.getRuntime();
        return runtime.getImage();
    }

    @Override
    protected String getServerId() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getServerId();
    }

    @Override
    protected String getRegistryUrl() {
        final RuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getRegistryUrl();
    }

    @Override
    protected String getSchemaVersion() {
        return "V2";
    }

    @Override
    protected WebContainer getWebContainer() throws MojoExecutionException {
        validate(validator.validateWebContainer());
        final RuntimeSetting runtime = mojo.getRuntime();
        return runtime.getWebContainer();
    }

    @Override
    protected JavaVersion getJavaVersion() throws MojoExecutionException {
        validate(validator.validateJavaVersion());
        final RuntimeSetting runtime = mojo.getRuntime();
        return runtime.getJavaVersion();
    }

    @Override
    protected List<Resource> getResources() {
        final Deployment deployment = mojo.getDeployment();
        return deployment == null ? null : deployment.getResources();
    }
}
