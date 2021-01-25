/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.webapp.utils.DeployUtils;
import com.microsoft.azure.maven.webapp.utils.Utils;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.toolkits.appservice.service.IAppService;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.tools.common.model.ResourceGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    private static final Path FTP_ROOT = Paths.get("/site/wwwroot");
    private static final String NO_RESOURCES_CONFIG = "<resources> is empty. Please make sure it is configured in pom.xml.";

    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App.";
    public static final String CREATE_DEPLOYMENT_SLOT = "Target Deployment Slot doesn't exist. Creating a new one...";
    public static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created the Deployment Slot.";
    public static final String UPDATE_WEBAPP = "Updating target Web App...";
    public static final String UPDATE_WEBAPP_SKIP = "No runtime configured. Skip the update.";
    public static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App.";

    public static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    public static final String SLOT_SHOULD_EXIST_NOW = "Target deployment slot still does not exist. " +
            "Please check if any error message during creation.";

    @Override
    protected void doExecute() throws AzureExecutionException {
        final WebAppConfig config = getWebAppConfig();
        final IAppService target = createOrUpdateResource(config);
        deploy(target, config);
    }

    private IAppService createOrUpdateResource(final WebAppConfig config) throws AzureExecutionException {
        if (StringUtils.isEmpty(config.getDeploymentSlotName())) {
            final IWebApp webApp = getWebApp(config);
            return webApp.exists() ? updateWebApp(webApp, config) : createWebApp(webApp, config);
        } else {
            final IWebAppDeploymentSlot slot = getDeploymentSlot(config);
            return slot.exists() ? updateDeploymentSlot(slot, config) : createDeploymentSlot(slot, config);
        }
    }

    private IWebApp getWebApp(final WebAppConfig config) {
        return az.webapp(config.getResourceGroup(), config.getAppName());
    }

    private IWebAppDeploymentSlot getDeploymentSlot(final WebAppConfig config) throws AzureExecutionException {
        final IWebApp webApp = getWebApp(config);
        final boolean isDeploymentSlot = StringUtils.isNotEmpty(config.getDeploymentSlotName());
        if (isDeploymentSlot && !webApp.exists()) {
            throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
        }
        return webApp.deploymentSlot(config.getDeploymentSlotName());
    }

    private IWebApp createWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) {
        // todo: get or create Resource Group
        final ResourceGroup resourceGroup = null;
        // Get or create App Service Plan
        final IAppServicePlan appServicePlan = az.appServicePlan(webAppConfig.getServicePlanResourceGroup(), webAppConfig.getServicePlanName());
        if (!appServicePlan.exists()) {
            appServicePlan.create()
                    .withName(webAppConfig.getServicePlanName())
                    .withResourceGroup(resourceGroup.getName())
                    .withRegion(webAppConfig.getRegion())
                    .withPricingTier(webAppConfig.getPricingTier())
                    .withOperatingSystem(webAppConfig.getRuntime().getOperatingSystem())
                    .commit();
        }
        return webApp.create().withName(webAppConfig.getAppName())
                .withResourceGroup(webAppConfig.getResourceGroup())
                .withPlan(appServicePlan.entity().getId())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withSubscription(webAppConfig.getSubscriptionId())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
    }

    private IWebApp updateWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) throws AzureExecutionException {
        // update app service plan
        final IAppServicePlan currentPlan = webApp.plan();
        final IAppServicePlan targetServicePlan = StringUtils.isEmpty(webAppConfig.getServicePlanName()) ? currentPlan :
                az.appServicePlan(webAppConfig.getServicePlanResourceGroup(), webAppConfig.getServicePlanName());
        if (!targetServicePlan.exists()) {
            throw new AzureExecutionException(String.format("App service plan %s was not found in resource group %s",
                    webAppConfig.getServicePlanName(), webAppConfig.getServicePlanResourceGroup()));
        }
        targetServicePlan.update().withPricingTier(webAppConfig.getPricingTier()).commit();
        return webApp.update().withPlan(targetServicePlan.entity().getId())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
    }

    private IWebAppDeploymentSlot createDeploymentSlot(final IWebAppDeploymentSlot slot, final WebAppConfig webAppConfig) {
        return slot.create().withName(webAppConfig.getDeploymentSlotName())
                .withConfigurationSource(webAppConfig.getDeploymentSlotConfigurationSource())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
    }

    private void deploy(IAppService target, WebAppConfig config) throws AzureExecutionException {
        try {
            if (isStopAppDuringDeployment()) {
                WebAppUtils.stopAppService(target);
            }
            deployArtifacts(target, config);
            deployExternalResources(target);
        } finally {
            WebAppUtils.startAppService(target);
        }
    }

    // update existing slot is not supported in current version, will implement it later
    private IWebAppDeploymentSlot updateDeploymentSlot(final IWebAppDeploymentSlot slot, final WebAppConfig webAppConfig) {
        return slot;
    }

    private void deployArtifacts(IAppService target, WebAppConfig config) throws AzureExecutionException {
        // This is the codes for one deploy API, for current release, will replace it with zip all files and deploy with zip deploy
        final List<Pair<File, DeployType>> resources = config.getResources();
        if (CollectionUtils.isEmpty(resources)) {
            return;
        }
        // call correspond deploy method when deploy artifact only
        if (resources.size() == 1) {
            final Pair<File, DeployType> resource = resources.get(0);
            target.deploy(resource.getRight(), resource.getLeft());
            return;
        }
        // package all resource and do zip deploy
        // todo: migrate to use one deploy
        final List<File> files = resources.stream().map(pair -> pair.getLeft()).collect(Collectors.toList());
        deployResourcesWithZipDeploy(target, files);
    }

    private void deployResourcesWithZipDeploy(IAppService target, List<File> files) throws AzureExecutionException {
        final File stagingDirectory = prepareStagingDirectory(files);
        // Rename jar once java_se runtime
        if (target.getRuntime().getWebContainer() == WebContainer.JAVA_SE) {
            DeployUtils.prepareJavaSERuntimeJarArtifact(files, project.getBuild().getFinalName());
        }
        final File zipFile = Utils.createTempFile(appName + UUID.randomUUID().toString(), ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        // Deploy zip with zip deploy
        target.deploy(DeployType.ZIP, zipFile);
    }

    private static File prepareStagingDirectory(List<File> files) throws AzureExecutionException {
        final File stagingDirectory = FileUtils.getTempDirectory();
        try {
            FileUtils.forceDeleteOnExit(stagingDirectory);
            // Copy resources to staging folder
            for (final File file : files) {
                FileUtils.copyFileToDirectory(file, stagingDirectory);
            }
            return stagingDirectory;
        } catch (IOException e) {
            throw new AzureExecutionException("Failed to package resources", e);
        }
    }

    private void deployExternalResources(IAppService target) throws AzureExecutionException {
        final List<Resource> resources = this.deployment == null ? null : this.deployment.getResources();
        final List<Resource> externalResources = resources.stream().filter(DeployMojo::isExternalResource).collect(Collectors.toList());
        DeployUtils.deployResourcesWithFtp(target, externalResources);
    }

    private static boolean isExternalResource(Resource resource) {
        final Path target = Paths.get(DeployUtils.getAbsoluteTargetPath(resource.getTargetPath()));
        return !target.startsWith(FTP_ROOT);
    }
}
