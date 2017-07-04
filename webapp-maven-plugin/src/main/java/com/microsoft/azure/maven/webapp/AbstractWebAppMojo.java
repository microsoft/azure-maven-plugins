/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import com.microsoft.azure.maven.webapp.configuration.PricingTierEnum;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base abstract class for shared configurations and operations.
 */
public abstract class AbstractWebAppMojo extends AbstractAzureMojo {
    @Parameter(property = "resourceGroup", required = true)
    protected String resourceGroup;

    @Parameter(property = "appName", required = true)
    protected String appName;

    @Parameter(property = "region", defaultValue = "westus")
    protected String region;

    @Parameter(property = "pricingTier", defaultValue = "S1")
    protected PricingTierEnum pricingTier;

    @Parameter(property = "javaVersion")
    protected String javaVersion;

    @Parameter(property = "javaWebContainer")
    protected String javaWebContainer;

    @Parameter(property = "containerSettings")
    protected ContainerSetting containerSettings;

    @Parameter(property = "appSettings")
    protected Properties appSettings;

    @Parameter(property = "deploymentType")
    protected String deploymentType;

    @Parameter(property = "resources")
    protected List<Resource> resources;

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public String getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier == null ? PricingTier.STANDARD_S1 : pricingTier.toPricingTier();
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
    }

    public WebContainer getJavaWebContainer() {
        return StringUtils.isEmpty(javaWebContainer) ? null : WebContainer.fromString(javaWebContainer);
    }

    public ContainerSetting getContainerSettings() {
        return containerSettings;
    }

    public Map getAppSettings() {
        return appSettings;
    }

    public DeploymentType getDeploymentType() throws MojoExecutionException {
        return DeploymentType.fromString(deploymentType);
    }

    public String getDeploymentStageDirectory() {
        return getProject().getBasedir().getAbsolutePath() + "/target/staged-" + getAppName();
    }

    public List<Resource> getResources() {
        return resources;
    }

    public WebApp getWebApp() {
        try {
            return getAzureClient().webApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (Exception e) {
            // Swallow exception for non-existing web app
        }
        return null;
    }
}
