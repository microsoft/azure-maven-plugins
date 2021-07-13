/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.utils.DeployUtils;
import com.microsoft.azure.maven.webapp.utils.Utils;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
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
    private static final String NO_RUNTIME_CONFIG = "You need to specified <runtime> in pom.xml for creating azure webapps.";
    private static final String CREATE_NEW_APP_SERVICE_PLAN = "createNewAppServicePlan";
    private static final String CREATE_NEW_RESOURCE_GROUP = "createNewResourceGroup";
    private static final String CREATE_NEW_WEB_APP = "createNewWebApp";
    private static final String CREATE_NEW_DEPLOYMENT_SLOT = "createNewDeploymentSlot";

    @Override
    protected void doExecute() throws AzureExecutionException {
        validateConfiguration(message -> AzureMessager.getMessager().error(message.getMessage()), true);
        // initialize library client
        az = getOrCreateAzureAppServiceClient();

        final WebAppConfig config = getWebAppConfig();
        final IWebAppBase target = createOrUpdateResource(config);
        deploy(target, config);
    }

    private IWebAppBase createOrUpdateResource(final WebAppConfig config) throws AzureExecutionException {
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

    private IWebApp createWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) throws AzureExecutionException {
        if (webAppConfig.getRuntime() == null) {
            throw new AzureExecutionException(NO_RUNTIME_CONFIG);
        }
        getTelemetryProxy().addDefaultProperty(CREATE_NEW_WEB_APP, String.valueOf(true));
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(webAppConfig);
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan(webAppConfig);
        AzureMessager.getMessager().info(String.format(CREATE_WEBAPP, webAppConfig.getAppName()));
        final IWebApp result = webApp.create().withName(webAppConfig.getAppName())
                .withResourceGroup(resourceGroup.name())
                .withPlan(appServicePlan.id())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        AzureMessager.getMessager().info(String.format(CREATE_WEB_APP_DONE, result.name()));
        return result;
    }

    private IWebApp updateWebApp(final IWebApp webApp, final WebAppConfig webAppConfig) {
        // update app service plan
        AzureMessager.getMessager().info(String.format(UPDATE_WEBAPP, webApp.name()));
        final IAppServicePlan currentPlan = webApp.plan();
        IAppServicePlan targetServicePlan = StringUtils.isEmpty(webAppConfig.getServicePlanName()) ? currentPlan :
                az.appServicePlan(getServicePlanResourceGroup(webAppConfig), webAppConfig.getServicePlanName());
        if (!targetServicePlan.exists()) {
            targetServicePlan = getOrCreateAppServicePlan(webAppConfig);
        } else if (webAppConfig.getPricingTier() != null) {
            targetServicePlan.update().withPricingTier(webAppConfig.getPricingTier()).commit();
        }

        final IWebApp result = webApp.update().withPlan(targetServicePlan.id())
                .withRuntime(webAppConfig.getRuntime())
                .withDockerConfiguration(webAppConfig.getDockerConfiguration())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        AzureMessager.getMessager().info(String.format(UPDATE_WEBAPP_DONE, webApp.name()));
        return result;
    }

    private ResourceGroup getOrCreateResourceGroup(final WebAppConfig webAppConfig) {
        // todo: Extract resource group logic to library
        final AzureResourceManager azureResourceManager = az.getAzureResourceManager(webAppConfig.getSubscriptionId());
        try {
            return azureResourceManager.resourceGroups().getByName(webAppConfig.getResourceGroup());
        } catch (ManagementException e) {
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP, webAppConfig.getResourceGroup(), webAppConfig.getRegion().getName()));
            getTelemetryProxy().addDefaultProperty(CREATE_NEW_RESOURCE_GROUP, String.valueOf(true));
            final ResourceGroup result = azureResourceManager.resourceGroups().define(webAppConfig.getResourceGroup())
                    .withRegion(webAppConfig.getRegion().getName()).create();
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP_DONE, webAppConfig.getResourceGroup()));
            return result;
        }
    }

    private IAppServicePlan getOrCreateAppServicePlan(final WebAppConfig webAppConfig) {
        final String servicePlanName = StringUtils.isEmpty(webAppConfig.getServicePlanName()) ?
                getNewAppServicePlanName(webAppConfig) : webAppConfig.getServicePlanName();
        final String servicePlanGroup = getServicePlanResourceGroup(webAppConfig);
        final IAppServicePlan appServicePlan = az.appServicePlan(servicePlanGroup, servicePlanName);
        if (!appServicePlan.exists()) {
            AzureMessager.getMessager().info(CREATE_APP_SERVICE_PLAN);
            getTelemetryProxy().addDefaultProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            appServicePlan.create()
                    .withName(servicePlanName)
                    .withResourceGroup(servicePlanGroup)
                    .withRegion(webAppConfig.getRegion())
                    .withPricingTier(webAppConfig.getPricingTier())
                    .withOperatingSystem(webAppConfig.getRuntime().getOperatingSystem())
                    .commit();
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_DONE, appServicePlan.name()));
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
        AzureMessager.getMessager().info(String.format(CREATE_DEPLOYMENT_SLOT, webAppConfig.getDeploymentSlotName(), webAppConfig.getAppName()));
        getTelemetryProxy().addDefaultProperty(CREATE_NEW_DEPLOYMENT_SLOT, String.valueOf(true));
        final IWebAppDeploymentSlot result = slot.create().withName(webAppConfig.getDeploymentSlotName())
                .withConfigurationSource(webAppConfig.getDeploymentSlotConfigurationSource())
                .withAppSettings(webAppConfig.getAppSettings())
                .commit();
        AzureMessager.getMessager().info(CREATE_DEPLOYMENT_SLOT_DONE);
        return result;
    }

    private void deploy(IWebAppBase target, WebAppConfig config) throws AzureExecutionException {
        if (target.getRuntime().isDocker()) {
            AzureMessager.getMessager().info(SKIP_DEPLOYMENT_FOR_DOCKER_APP_SERVICE);
            return;
        }
        try {
            AzureMessager.getMessager().info(String.format(DEPLOY_START, config.getAppName()));
            if (isStopAppDuringDeployment()) {
                WebAppUtils.stopAppService(target);
            }
            deployArtifacts(target, config);
            deployExternalResources(target);
            AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, target.hostName()));
        } finally {
            WebAppUtils.startAppService(target);
        }
    }

    // update existing slot is not supported in current version, will implement it later
    private IWebAppDeploymentSlot updateDeploymentSlot(final IWebAppDeploymentSlot slot, final WebAppConfig webAppConfig) {
        return slot;
    }

    private void deployArtifacts(IWebAppBase target, WebAppConfig config) throws AzureExecutionException {
        final List<WebAppArtifact> artifactsOneDeploy = config.getWebAppArtifacts().stream()
                .filter(artifact -> artifact.getDeployType() != null)
                .collect(Collectors.toList());
        artifactsOneDeploy.forEach(resource -> target.deploy(resource.getDeployType(), resource.getFile(), resource.getPath()));

        // This is the codes for one deploy API, for current release, will replace it with zip all files and deploy with zip deploy
        final List<WebAppArtifact> artifacts = config.getWebAppArtifacts().stream()
                .filter(artifact -> artifact.getDeployType() == null)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(artifacts)) {
            return;
        }
        // call correspond deploy method when deploy artifact only
        if (artifacts.size() == 1) {
            final WebAppArtifact artifact = artifacts.get(0);
            final DeployType deployType = DeployType.getDeployTypeFromFile(artifact.getFile());
            target.deploy(deployType, artifact.getFile(), artifact.getPath());
            return;
        }
        // Support deploy multi war to different paths
        if (DeployUtils.isAllWarArtifacts(artifacts)) {
            artifacts.forEach(resource -> target.deploy(DeployType.getDeployTypeFromFile(resource.getFile()), resource.getFile(), resource.getPath()));
            return;
        }
        // package all resource and do zip deploy
        // todo: migrate to use one deploy
        deployArtifactsWithZipDeploy(target, artifacts);
    }

    private void deployArtifactsWithZipDeploy(IWebAppBase target, List<WebAppArtifact> artifacts) throws AzureExecutionException {
        final File stagingDirectory = prepareStagingDirectory(artifacts);
        // Rename jar once java_se runtime
        if (Objects.equals(target.getRuntime().getWebContainer(), WebContainer.JAVA_SE)) {
            final List<File> files = new ArrayList<>(FileUtils.listFiles(stagingDirectory, null, true));
            DeployUtils.prepareJavaSERuntimeJarArtifact(files, project.getBuild().getFinalName());
        }
        final File zipFile = Utils.createTempFile(appName + UUID.randomUUID(), ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        // Deploy zip with zip deploy
        target.deploy(DeployType.ZIP, zipFile);
    }

    private static File prepareStagingDirectory(List<WebAppArtifact> webAppArtifacts) throws AzureExecutionException {
        try {
            final File stagingDirectory = Files.createTempDirectory("azure-functions").toFile();
            FileUtils.forceDeleteOnExit(stagingDirectory);
            // Copy maven artifacts to staging folder
            for (final WebAppArtifact webAppArtifact : webAppArtifacts) {
                final File targetFolder = StringUtils.isEmpty(webAppArtifact.getPath()) ? stagingDirectory :
                        new File(stagingDirectory, webAppArtifact.getPath());
                FileUtils.copyFileToDirectory(webAppArtifact.getFile(), targetFolder);
            }
            return stagingDirectory;
        } catch (IOException e) {
            throw new AzureExecutionException("Failed to package resources", e);
        }
    }

    private void deployExternalResources(IAppService target) throws AzureExecutionException {
        DeployUtils.deployResourcesWithFtp(target, filterResources(DeploymentResource::isExternalResource));
    }

    private List<DeploymentResource> filterResources(Predicate<DeploymentResource> predicate) {
        final List<DeploymentResource> resources = this.deployment == null ? Collections.emptyList() : this.deployment.getResources();
        return resources.stream()
                .filter(predicate).collect(Collectors.toList());
    }
}
