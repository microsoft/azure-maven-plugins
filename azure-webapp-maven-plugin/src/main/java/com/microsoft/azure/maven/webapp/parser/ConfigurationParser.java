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
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.util.List;

public abstract class ConfigurationParser {
    protected final AbstractWebAppMojo mojo;

    protected ConfigurationParser(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    protected String getAppName() throws MojoExecutionException {
        final String appName = mojo.getAppName();
        if (StringUtils.isEmpty(appName)) {
            throw new MojoExecutionException("Please config the <appName> in pom.xml.");
        }
        if (appName.startsWith("-") || !appName.matches("[a-zA-Z0-9\\-]{2,60}")) {
            throw new MojoExecutionException("The <appName> only allow alphanumeric characters, " +
                "hyphens and cannot start or end in a hyphen.");
        }
        return mojo.getAppName();
    }

    protected String getResourceGroup() throws MojoExecutionException {
        final String resourceGroupName = mojo.getResourceGroup();
        if (StringUtils.isEmpty(resourceGroupName)) {
            throw new MojoExecutionException("Please config the <resourceGroup> in pom.xml.");
        }
        if (resourceGroupName.endsWith(".") || !resourceGroupName.matches("[a-zA-Z0-9\\.\\_\\-\\(\\)]{1,90}")) {
            throw new MojoExecutionException("The <resourceGroup> only allow alphanumeric characters, periods, " +
                "underscores, hyphens and parenthesis and cannot end in a period.");
        }
        return mojo.getResourceGroup();
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
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
            .deploymentSlotSetting(mojo.getDeploymentSlotSetting())
            .os(getOs())
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
