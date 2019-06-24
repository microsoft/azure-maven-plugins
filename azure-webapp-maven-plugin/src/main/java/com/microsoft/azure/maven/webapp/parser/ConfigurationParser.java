/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

public abstract class ConfigurationParser {
    protected final AbstractWebAppMojo mojo;
    protected final AbstractConfigurationValidator validator;

    protected ConfigurationParser(final AbstractWebAppMojo mojo, final AbstractConfigurationValidator validator) {
        this.mojo = mojo;
        this.validator = validator;
    }

    protected String getAppName() throws MojoExecutionException {
        validate(validator.validateAppName());
        return mojo.getAppName();
    }

    protected String getResourceGroup() throws MojoExecutionException {
        validate(validator.validateResourceGroup());
        return mojo.getResourceGroup();
    }

    protected PricingTier getPricingTier() throws MojoExecutionException{
        validate(validator.validatePricingTier());
        final PricingTier pricingTier = AppServiceUtils.getPricingTierFromString(mojo.getPricingTier());
        return pricingTier == null ? WebAppConfiguration.DEFAULT_PRICINGTIER : pricingTier;
    }

    protected abstract OperatingSystemEnum getOs() throws MojoExecutionException;

    protected abstract Region getRegion() throws MojoExecutionException;

    protected abstract RuntimeStack getRuntimeStack() throws MojoExecutionException;

    protected abstract String getImage() throws MojoExecutionException;

    protected abstract String getServerId() throws MojoExecutionException;

    protected abstract String getRegistryUrl();

    protected abstract String getSchemaVersion();

    protected abstract JavaVersion getJavaVersion() throws MojoExecutionException;

    protected abstract WebContainer getWebContainer() throws MojoExecutionException;

    protected abstract List<Resource> getResources() throws MojoExecutionException;

    protected void validate(String errorMessage) throws MojoExecutionException {
        if (errorMessage != null) {
            throw new MojoExecutionException(errorMessage);
        }
    }

    public WebAppConfiguration getWebAppConfiguration() throws MojoExecutionException {
        WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        final OperatingSystemEnum os = getOs();
        if (os == null) {
            mojo.getLog().debug("No runtime related config is specified. " +
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
                    throw new MojoExecutionException("Invalid operating system from the configuration.");
            }
        }

        return builder.appName(getAppName())
            .resourceGroup(getResourceGroup())
            .region(getRegion())
            .pricingTier(getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
            .deploymentSlotSetting(mojo.getDeploymentSlotSetting())
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
