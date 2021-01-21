/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;
import lombok.AllArgsConstructor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.util.List;

@AllArgsConstructor
public abstract class AbstractConfigParser {

    protected AbstractWebAppMojo mojo;

    public String getAppName() {
        return mojo.getAppName();
    }

    public String getResourceGroup() {
        return mojo.getResourceGroup();
    }

    public String getDeploymentSlotName() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getName();
    }

    public String getDeploymentSlotConfigurationSource() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getConfigurationSource();
    }

    public PricingTier getPricingTier() {
        return PricingTier.fromString(mojo.getPricingTier());
    }

    public String getAppServicePlanName() {
        return mojo.getAppServicePlanName();
    }

    public String getAppServicePlanResourceGroup() {
        return mojo.getAppServicePlanResourceGroup();
    }

    public String getStagingDirectoryPath() {
        return mojo.getDeploymentStagingDirectoryPath();
    }

    public String getBuildDirectoryAbsolutePath() {
        return mojo.getBuildDirectoryAbsolutePath();
    }

    public MavenProject getProject() {
        return mojo.getProject();
    }

    public MavenSession getSession() {
        return mojo.getSession();
    }

    public MavenResourcesFiltering getResourcesFiltering() {
        return mojo.getMavenResourcesFiltering();
    }

    public abstract Runtime getRuntime();

    public abstract Region getRegion();

    public abstract DeployType getDeployType();

    public abstract DockerConfiguration getDockerConfiguration();

    public abstract String getSchemaVersion();

    public abstract List<Resource> getResources();
}
