/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import java.util.List;

public class V2ConfigurationParser extends ConfigurationParser {

    public V2ConfigurationParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    protected OperatingSystem getOs() throws AzureExecutionException {
        validate(validator.validateOs());
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        final String os = runtime.getOs();
        if (runtime.isEmpty()) {
            return null;
        }
        return OperatingSystem.fromString(os);
    }

    @Override
    protected Region getRegion() throws AzureExecutionException {
        validate(validator.validateRegion());
        final String region = mojo.getRegion();
        return Region.fromName(region);
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
    protected List<DeploymentResource> getResources() {
        final Deployment deployment = mojo.getDeployment();
        return deployment == null ? null : deployment.getResources();
    }
}
