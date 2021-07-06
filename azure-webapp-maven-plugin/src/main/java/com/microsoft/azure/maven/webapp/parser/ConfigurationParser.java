/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;

import java.util.List;
import java.util.Objects;

public class ConfigurationParser {
    protected final AbstractWebAppMojo mojo;

    public ConfigurationParser(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    protected String getAppName() throws AzureExecutionException {
        return mojo.getAppName();
    }

    protected String getResourceGroup() throws AzureExecutionException {
        return mojo.getResourceGroup();
    }

    protected String getPricingTier() throws AzureExecutionException {
        return mojo.getPricingTier();
    }

    protected DeploymentSlotSetting getDeploymentSlotSetting() throws AzureExecutionException {
        return mojo.getDeploymentSlotSetting();
    }

    protected OperatingSystem getOs() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        final String os = runtime.getOs();
        if (runtime.isEmpty()) {
            return null;
        }
        return OperatingSystem.fromString(os);
    }

    protected Region getRegion() throws AzureExecutionException {
        final String region = mojo.getRegion();
        return Region.fromName(region);
    }

    protected String getImage() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getImage();
    }

    protected String getServerId() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getServerId();
    }

    protected String getRegistryUrl() {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        return runtime.getRegistryUrl();
    }

    protected String getSchemaVersion() {
        return "v2";
    }

    protected WebContainer getWebContainer() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getWebContainer();
    }

    protected JavaVersion getJavaVersion() throws AzureExecutionException {
        final MavenRuntimeConfig runtime = mojo.getRuntime();
        return runtime.getJavaVersion();
    }

    protected List<DeploymentResource> getResources() {
        final Deployment deployment = mojo.getDeployment();
        return deployment == null ? null : deployment.getResources();
    }

    public WebAppConfiguration getWebAppConfiguration() throws AzureExecutionException {
        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
        final OperatingSystem os = getOs();
        if (os == null) {
            Log.debug("No runtime related config is specified. " +
                "It will cause error if creating a new web app.");
        } else {
            switch (os) {
                case WINDOWS:
                case LINUX:
                    builder = builder.javaVersion(Objects.toString(getJavaVersion())).webContainer(Objects.toString(getWebContainer()));
                    break;
                case DOCKER:
                    builder = builder.image(getImage()).serverId(getServerId()).registryUrl(getRegistryUrl());
                    break;
                default:
                    throw new AzureExecutionException("Invalid operating system from the configuration.");
            }
        }
        return builder.appName(getAppName())
            .resourceGroup(getResourceGroup())
            .region(getRegion())
            .pricingTier(getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
            .deploymentSlotSetting(getDeploymentSlotSetting())
            .os(os)
            .mavenSettings(mojo.getSettings())
            .resources(getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .buildDirectoryAbsolutePath(mojo.getBuildDirectoryAbsolutePath())
            .project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .schemaVersion(getSchemaVersion())
            .build();
    }
}
