/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.webapp.utils.DeployUtils;
import com.microsoft.azure.maven.webapp.utils.Utils;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.toolkits.appservice.service.IAppService;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;
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
import java.nio.file.Files;
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
    private static final String CREATE_WEBAPP = "Creating web app %s...";
    private static final String CREATE_WEB_APP_DONE = "Successfully created Web App %s.";
    private static final String UPDATE_WEBAPP = "Updating target Web App %s...";
    private static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App %s.";
    private static final String CREATE_RESOURCE_GROUP = "Creating resource group %s in region %s...";
    private static final String CREATE_RESOURCE_GROUP_DONE = "Successfully created resource group %s.";
    private static final String CREATE_APP_SERVICE_PLAN = "Creating app service plan...";
    private static final String CREATE_APP_SERVICE_DONE = "Successfully created app service plan %s.";
    private static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    private static final String CREATE_DEPLOYMENT_SLOT = "Creating deployment slot %s in web app %s";
    private static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created the Deployment Slot.";
    private static final String DEPLOY_START = "Trying to deploy artifact to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    private static final String SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE = "Skip deployment for docker app service";

    @Override
    protected void doExecute() throws AzureExecutionException {
        // initialize library client
        az = getOrCreateAzureAppServiceClient();

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
        if (!webApp.exists()) {
            throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
        }
        return webApp.deploymentSlot(config.getDeploymentSlotName());
    }

    private IWebApp createWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) {
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(webAppConfig);
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan(webAppConfig);

        Log.info(String.format(CREATE_WEBAPP, webAppConfig.getAppName()));
        final IWebApp result = webApp.create().withName(webAppConfig.getAppName())
                .withResourceGroup(resourceGroup.name())
                .withPlan(appServicePlan.id())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        Log.info(String.format(CREATE_WEB_APP_DONE, result.name()));
        return result;
    }

    private IWebApp updateWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) {
        // update app service plan
        Log.info(String.format(UPDATE_WEBAPP, webApp.name()));
        final IAppServicePlan currentPlan = webApp.plan();
        IAppServicePlan targetServicePlan = StringUtils.isEmpty(webAppConfig.getServicePlanName()) ? currentPlan :
                az.appServicePlan(getServicePlanResourceGroup(webAppConfig), webAppConfig.getServicePlanName());
        if (!targetServicePlan.exists()) {
            targetServicePlan = getOrCreateAppServicePlan(webAppConfig);
        }
        targetServicePlan.update().withPricingTier(webAppConfig.getPricingTier()).commit();
        final IWebApp result = webApp.update().withPlan(targetServicePlan.id())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        Log.info(String.format(UPDATE_WEBAPP_DONE, webApp.name()));
        return result;
    }

    // todo: Extract resource group logic to library
    private ResourceGroup getOrCreateResourceGroup(final WebAppConfig webAppConfig) {
        try {
            return az.getAzureResourceManager().resourceGroups().getByName(webAppConfig.getResourceGroup());
        } catch (Exception e) {
            Log.info(String.format(CREATE_RESOURCE_GROUP, webAppConfig.getResourceGroup(), webAppConfig.getRegion().getName()));
            final ResourceGroup result = az.getAzureResourceManager().resourceGroups().define(webAppConfig.getResourceGroup())
                    .withRegion(webAppConfig.getRegion().getName()).create();
            Log.info(String.format(CREATE_RESOURCE_GROUP_DONE, webAppConfig.getResourceGroup()));
            return result;
        }
    }

    private IAppServicePlan getOrCreateAppServicePlan(final WebAppConfig webAppConfig) {
        final String servicePlanName = StringUtils.isEmpty(webAppConfig.getServicePlanName()) ?
                getNewAppServicePlanName(webAppConfig) : webAppConfig.getServicePlanName();
        final String servicePlanGroup = getServicePlanResourceGroup(webAppConfig);
        final IAppServicePlan appServicePlan = az.appServicePlan(servicePlanGroup, servicePlanName);
        if (!appServicePlan.exists()) {
            Log.info(CREATE_APP_SERVICE_PLAN);
            appServicePlan.create()
                    .withName(servicePlanName)
                    .withResourceGroup(servicePlanGroup)
                    .withRegion(webAppConfig.getRegion())
                    .withPricingTier(webAppConfig.getPricingTier())
                    .withOperatingSystem(webAppConfig.getRuntime().getOperatingSystem())
                    .commit();
            Log.info(String.format(CREATE_APP_SERVICE_DONE, appServicePlan.name()));
        }
        return appServicePlan;
    }

    private String getNewAppServicePlanName(final WebAppConfig webAppConfig) {
        return StringUtils.isEmpty(webAppConfig.getServicePlanName()) ? String.format("asp-%s", webAppConfig.getAppName()) :
                webAppConfig.getServicePlanName();
    }

    private String getServicePlanResourceGroup(final WebAppConfig webAppConfig) {
        return StringUtils.isEmpty(webAppConfig.getServicePlanResourceGroup()) ? webAppConfig.getResourceGroup() :
                webAppConfig.getServicePlanResourceGroup();
    }

    private IWebAppDeploymentSlot createDeploymentSlot(final IWebAppDeploymentSlot slot, final WebAppConfig webAppConfig) {
        Log.info(String.format(CREATE_DEPLOYMENT_SLOT, webAppConfig.getDeploymentSlotName(), webAppConfig.getAppName()));
        final IWebAppDeploymentSlot result = slot.create().withName(webAppConfig.getDeploymentSlotName())
                .withConfigurationSource(webAppConfig.getDeploymentSlotConfigurationSource())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        Log.info(CREATE_DEPLOYMENT_SLOT_DONE);
        return result;
    }

    private void deploy(IAppService target, WebAppConfig config) throws AzureExecutionException {
        if (target.getRuntime().getOperatingSystem() == OperatingSystem.DOCKER) {
            Log.info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
        }
        try {
            Log.info(String.format(DEPLOY_START, config.getAppName()));
            if (isStopAppDuringDeployment()) {
                WebAppUtils.stopAppService(target);
            }
            deployArtifacts(target, config);
            deployExternalResources(target);
            Log.info(String.format(DEPLOY_FINISH, target.hostName()));
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
        try {
            final File stagingDirectory = Files.createTempDirectory("azure-functions").toFile();
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
