/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;

import org.apache.maven.model.Resource;

import java.util.List;

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

    protected PricingTier getPricingTier() throws AzureExecutionException {
        validate(validator.validatePricingTier());
        return AppServiceUtils.getPricingTierFromString(mojo.getPricingTier());
    }

    protected DeploymentSlotSetting getDeploymentSlotSetting() throws AzureExecutionException {
        validate(validator.validateDeploymentSlot());
        return mojo.getDeploymentSlotSetting();
    }

    protected abstract OperatingSystemEnum getOs() throws AzureExecutionException;

    protected abstract Region getRegion() throws AzureExecutionException;

    protected abstract RuntimeStack getRuntimeStack() throws AzureExecutionException;

    protected abstract String getImage() throws AzureExecutionException;

    protected abstract String getServerId() throws AzureExecutionException;

    protected abstract String getRegistryUrl();

    protected abstract String getSchemaVersion();

    protected abstract JavaVersion getJavaVersion() throws AzureExecutionException;

    protected abstract WebContainer getWebContainer() throws AzureExecutionException;

    protected abstract List<Resource> getResources() throws AzureExecutionException;

    protected void validate(String errorMessage) throws AzureExecutionException {
        if (errorMessage != null) {
            throw new AzureExecutionException(errorMessage);
        }
    }

    public WebAppConfiguration getWebAppConfiguration() throws AzureExecutionException {
        WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        final OperatingSystemEnum os = getOs();
        if (os == null) {
            Log.debug("No runtime related config is specified. " +
                "It will cause error if creating a new web app.");
        } else {
            switch (os) {
                case Windows:
                    builder = builder.javaVersion(getJavaVersion()).webContainer(getWebContainer());
                    break;
                case Linux:
                    builder = builder.runtimeStack(getRuntimeStack());
                    break;
                case Docker:
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
