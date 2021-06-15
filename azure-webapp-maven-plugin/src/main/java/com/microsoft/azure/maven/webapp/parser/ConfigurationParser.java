/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;

import java.util.List;

@Deprecated
public abstract class ConfigurationParser {
    protected final AbstractWebAppMojo mojo;
    protected final AbstractConfigurationValidator validator;

    protected ConfigurationParser(final AbstractWebAppMojo mojo, final AbstractConfigurationValidator validator) {
        this.mojo = mojo;
        this.validator = validator;
    }

    protected String getAppName() throws AzureExecutionException {
        validate(validator.validateAppName());
        return mojo.getAppName();
    }

    protected String getResourceGroup() throws AzureExecutionException {
        validate(validator.validateResourceGroup());
        return mojo.getResourceGroup();
    }

    protected String getPricingTier() throws AzureExecutionException {
        validate(validator.validatePricingTier());
        return mojo.getPricingTier();
    }

    protected DeploymentSlotSetting getDeploymentSlotSetting() throws AzureExecutionException {
        validate(validator.validateDeploymentSlot());
        return mojo.getDeploymentSlotSetting();
    }

    protected abstract OperatingSystem getOs() throws AzureExecutionException;

    protected abstract Region getRegion() throws AzureExecutionException;

    protected abstract String getImage() throws AzureExecutionException;

    protected abstract String getServerId() throws AzureExecutionException;

    protected abstract String getRegistryUrl();

    protected abstract String getSchemaVersion();

    protected abstract JavaVersion getJavaVersion() throws AzureExecutionException;

    protected abstract WebContainer getWebContainer() throws AzureExecutionException;

    protected abstract List<DeploymentResource> getResources() throws AzureExecutionException;

    protected void validate(String errorMessage) throws AzureExecutionException {
        if (errorMessage != null) {
            throw new AzureExecutionException(errorMessage);
        }
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
                    builder = builder.javaVersion(getJavaVersion()).webContainer(getWebContainer());
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
