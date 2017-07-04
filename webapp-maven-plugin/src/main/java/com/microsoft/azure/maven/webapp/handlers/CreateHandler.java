/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.fluentcore.arm.models.GroupableResource;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CreateHandler implements Handler {
    private DeployMojo mojo;

    private WebApp.DefinitionStages.WithCreate app;

    public CreateHandler(DeployMojo mojo) {
        this.mojo = mojo;
        this.app = null;
    }

    @Override
    public Handler setupRuntime() throws MojoExecutionException {
        final GroupableResource.DefinitionStages.WithGroup withGroup = defineAppWithRegion();
        final WebApp.DefinitionStages.WithNewAppServicePlan withPlan = defineGroup(withGroup);
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final JavaVersion javaVersion = mojo.getJavaVersion();
        if (containerSetting == null && javaVersion == null) {
            throw new MojoExecutionException(
                    "Configuration conflict. You can not specify both <javaVersion> and <containerSettings>.");
        }

        if (containerSetting == null) {
            app = defineWebContainer(withPlan);
        } else {
            app = defineDockerContainerImage(withPlan);
        }

        return this;
    }

    @Override
    public Handler applySettings() throws Exception {
        final Map appSettings = mojo.getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            app.withAppSettings(appSettings);
        }

        final DeploymentType deploymentType = mojo.getDeploymentType();
        if (deploymentType == DeploymentType.LOCAL_GIT) {
            app.withLocalGitSourceControl();
        }

        return this;
    }

    @Override
    public Handler deployArtifacts() throws Exception {
        if (mojo.getResources() == null || mojo.getResources().isEmpty()) {
            mojo.getLog().info("No resources are specified. Skip artifacts deployment.");
        } else {
            stageArtifactsLocally();
            uploadArtifacts();
        }
        return this;
    }

    @Override
    public Handler commit() throws Exception {
        app.create();
        return this;
    }

    private void stageArtifactsLocally() throws IOException {
        mojo.getLog().info("Output directory: " + mojo.getDeploymentStageDirectory());
        for (final Resource resource : mojo.getResources()) {
            resource.setTargetPath(mojo.getDeploymentStageDirectory() + resource.getTargetPath());
        }

        final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
                mojo.getResources(),
                new File(mojo.getDeploymentStageDirectory()),
                mojo.getProject(),
                "UTF-8",
                null,
                Collections.EMPTY_LIST,
                mojo.getSession()
        );

        // Configure executor
        mavenResourcesExecution.setEscapeWindowsPaths(true);
        mavenResourcesExecution.setInjectProjectBuildFilters(false);
        mavenResourcesExecution.setOverwrite(true);
        mavenResourcesExecution.setIncludeEmptyDirs(false);
        mavenResourcesExecution.setSupportMultiLineFiltering(false);

        // Filter resources
        try {
            mojo.getMavenResourcesFiltering().filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException ex) {
            throw new IOException("Failed to copy resources", ex);
        }
    }

    private void uploadArtifacts() {
        final WebApp app = mojo.getWebApp();
        final PublishingProfile profile = app.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        new FTPUploader(mojo.getLog()).uploadDirectory(
                serverUrl,
                profile.ftpUsername(),
                profile.ftpPassword(),
                mojo.getDeploymentStageDirectory(),
                "/site/wwwroot");
    }

    private GroupableResource.DefinitionStages.WithGroup defineAppWithRegion() {
        final String appName = mojo.getAppName();

        final String region = mojo.getRegion();
        return mojo.getAzureClient().webApps().define(appName).withRegion(region);
    }

    private WebApp.DefinitionStages.WithNewAppServicePlan defineGroup(
            GroupableResource.DefinitionStages.WithGroup app) {
        final String resourceGroup = mojo.getResourceGroup();
        if (mojo.getAzureClient().resourceGroups().checkExistence(resourceGroup)) {
            return (WebApp.DefinitionStages.WithNewAppServicePlan) app.withExistingResourceGroup(resourceGroup);
        } else {
            return (WebApp.DefinitionStages.WithNewAppServicePlan) app.withNewResourceGroup(resourceGroup);
        }
    }

    private WebApp.DefinitionStages.WithCreate defineWebContainer(WebApp.DefinitionStages.WithNewAppServicePlan app) {
        final JavaVersion javaVersion = mojo.getJavaVersion();
        final PricingTier pricingTier = mojo.getPricingTier();
        final WebApp.DefinitionStages.WithCreate ret = app.withNewWindowsPlan(pricingTier);
        ret.withJavaVersion(javaVersion).withWebContainer(WebContainer.TOMCAT_8_5_NEWEST);
        return ret;
    }

    private WebApp.DefinitionStages.WithCreate defineDockerContainerImage(
            WebApp.DefinitionStages.WithNewAppServicePlan app) {
        final PricingTier pricingTier = mojo.getPricingTier();
        final WebApp.DefinitionStages.WithDockerContainerImage defWithDocker = app.withNewLinuxPlan(pricingTier);
        final ContainerSetting containerSetting = mojo.getContainerSettings();

        if (StringUtils.isEmpty(containerSetting.getServerId())) {
            return defWithDocker.withPublicDockerHubImage(containerSetting.getImageName());
        }

        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (StringUtils.isEmpty(containerSetting.getRegistryUrl())) {
            return defWithDocker.
                    withPrivateDockerHubImage(containerSetting.getImageName())
                    .withCredentials(server.getUsername(), server.getPassword());
        }

        return defWithDocker
                .withPrivateRegistryImage(containerSetting.getImageName(), containerSetting.getRegistryUrl())
                .withCredentials(server.getUsername(), server.getPassword());
    }
}
