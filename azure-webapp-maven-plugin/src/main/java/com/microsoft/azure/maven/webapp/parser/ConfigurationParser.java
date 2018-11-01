/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
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
        if (StringUtils.isEmpty(mojo.getAppName())) {
            throw new MojoExecutionException("Please config the <appName> in pom.xml.");
        }
        return mojo.getAppName();
    }

    protected String getResourceGroup() throws MojoExecutionException {
        if (StringUtils.isEmpty(mojo.getResourceGroup())) {
            throw new MojoExecutionException("Please config the <resourceGroup> in pom.xml.");
        }
        return mojo.getResourceGroup();
    }

    protected abstract OperatingSystemEnum getOs() throws MojoExecutionException;

    protected abstract String getRegion() throws MojoExecutionException;

    protected abstract RuntimeStack getRuntimeStack() throws MojoExecutionException;

    protected abstract String getImage() throws MojoExecutionException;

    protected abstract String getServerId() throws MojoExecutionException;

    protected abstract String getRegistryUrl();

    protected abstract JavaVersion getJavaVersion() throws MojoExecutionException;

    protected abstract WebContainer getWebContainer() throws MojoExecutionException;

    protected abstract List<Resource> getResources();

    public WebAppConfiguration getWebAppConfiguration() throws MojoExecutionException {
        WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        if (OperatingSystemEnum.Windows == getOs()) {
            builder = builder.javaVersion(getJavaVersion()).webContainer(getWebContainer());
        }
        if (OperatingSystemEnum.Linux == getOs()) {
            builder = builder.runtimeStack(getRuntimeStack());
        }
        if (OperatingSystemEnum.Docker == getOs()) {
            builder = builder.image(getImage()).serverId(getServerId()).registryUrl(getRegistryUrl());
        }

        return builder.appName(getAppName())
            .resourceGroup(getResourceGroup())
            .region(getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup(mojo.getAppServicePlanResourceGroup())
            .os(getOs())
            .mavenSettings(mojo.getSettings())
            .resources(getResources())
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .buildDirectoryAbsolutePath(mojo.getBuildDirectoryAbsolutePath())
            .project(mojo.getProject())
            .session(mojo.getSession())
            .filtering(mojo.getMavenResourcesFiltering())
            .build();
    }
}
