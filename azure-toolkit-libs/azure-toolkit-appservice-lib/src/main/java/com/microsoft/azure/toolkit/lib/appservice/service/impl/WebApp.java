/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WebApp implements IWebApp {

    private static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
    private WebAppEntity entity;
    private AzureAppService azureAppService;

    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.WebApp webAppInner;

    public WebApp(WebAppEntity entity, AzureResourceManager azureClient) {
        this.entity = entity;
        this.azureClient = azureClient;
    }

    @Override
    public WebAppEntity entity() {
        return entity;
    }

    @Override
    public IAppServicePlan plan() {
        return azureAppService.appServicePlan(getWebAppInner().appServicePlanId());
    }

    @Override
    public WebAppCreator create() {
        return new WebAppCreator();
    }

    @Override
    public void start() {
        getWebAppInner().start();
    }

    @Override
    public void stop() {
        getWebAppInner().stop();
    }

    @Override
    public void restart() {
        getWebAppInner().restart();
    }

    @Override
    public void delete() {
        azureClient.webApps().deleteById(getWebAppInner().id());
    }

    @Override
    public void deploy(DeployType deployType, File targetFile, String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        getWebAppInner().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public boolean exists() {
        refreshWebAppInner();
        return webAppInner != null;
    }

    @Override
    public String hostName() {
        return getWebAppInner().defaultHostname();
    }

    @Override
    public String state() {
        return getWebAppInner().state();
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        final com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile = getWebAppInner().getPublishingProfile();
        return PublishingProfile.createFromServiceModel(publishingProfile);
    }

    @Override
    public Runtime getRuntime() {
        return AppServiceUtils.getRuntimeFromWebApp(getWebAppInner());
    }

    @Override
    public WebAppUpdater update() {
        return new WebAppUpdater();
    }

    @Override
    public IWebAppDeploymentSlot deploymentSlot(String slotName) {
        final WebAppDeploymentSlotEntity slotEntity = WebAppDeploymentSlotEntity.builder().name(slotName)
            .resourceGroup(getWebAppInner().resourceGroupName())
            .webappName(getWebAppInner().name()).build();
        return new WebAppDeploymentSlot(slotEntity, azureClient);
    }

    @Override
    public List<IWebAppDeploymentSlot> deploymentSlots() {
        return getWebAppInner().deploymentSlots().list().stream()
            .map(slot -> new WebAppDeploymentSlot(WebAppDeploymentSlotEntity.builder().id(slot.id()).build(), azureClient))
            .collect(Collectors.toList());
    }

    private com.azure.resourcemanager.appservice.models.WebApp getWebAppInner() {
        if (webAppInner == null) {
            refreshWebAppInner();
        }
        return webAppInner;
    }

    synchronized void refreshWebAppInner() {
        try {
            webAppInner = StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.webApps().getById(entity.getId()) :
                azureClient.webApps().getByResourceGroup(entity.getResourceGroup(), entity.getName());
            entity = AppServiceUtils.fromWebApp(webAppInner);
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            webAppInner = null;
        }
    }

    @Override
    public String id() {
        return getWebAppInner().id();
    }

    @Override
    public String name() {
        return getWebAppInner().name();
    }

    public class WebAppCreator extends AbstractAppServiceCreator<WebApp> {
        // todo: Add validation for required parameters
        @Override
        public WebApp commit() {
            final DefinitionStages.Blank blank = WebApp.this.azureClient.webApps().define(getName());
            final Runtime runtime = getRuntime();
            final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlan(getAppServicePlanEntity(), azureClient);
            final ResourceGroup resourceGroup = WebApp.this.azureClient.resourceGroups().getByName(getResourceGroup());
            final DefinitionStages.WithCreate withCreate;
            switch (runtime.getOperatingSystem()) {
                case LINUX:
                    withCreate = createLinuxWebApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case WINDOWS:
                    withCreate = createWindowsWebApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case DOCKER:
                    final DockerConfiguration dockerConfiguration = getDockerConfiguration().get();
                    withCreate = createDockerWebApp(blank, resourceGroup, appServicePlan, dockerConfiguration);
                    break;
                default:
                    throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, runtime.getOperatingSystem()));
            }
            if (getAppSettings() != null && getAppSettings().isPresent()) {
                // todo: support remove app settings
                withCreate.withAppSettings(getAppSettings().get());
            }
            WebApp.this.webAppInner = withCreate.create();
            WebApp.this.entity = AppServiceUtils.fromWebApp(WebApp.this.webAppInner);
            return WebApp.this;
        }

        DefinitionStages.WithCreate createWindowsWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                        Runtime runtime) {
            return (DefinitionStages.WithCreate) blank.withExistingWindowsPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withJavaVersion(AppServiceUtils.toWindowsJavaVersion(runtime))
                .withWebContainer(AppServiceUtils.toWindowsWebContainer(runtime));
        }

        DefinitionStages.WithCreate createLinuxWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                      Runtime runtime) {
            return blank.withExistingLinuxPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withBuiltInImage(AppServiceUtils.toLinuxRuntimeStack(runtime));
        }

        DefinitionStages.WithCreate createDockerWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                       DockerConfiguration dockerConfiguration) {
            final DefinitionStages.WithLinuxAppFramework withLinuxAppFramework =
                blank.withExistingLinuxPlan(appServicePlan).withExistingResourceGroup(resourceGroup);
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                return withLinuxAppFramework.withPublicDockerHubImage(dockerConfiguration.getImage());
            }
            if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                return withLinuxAppFramework.withPrivateDockerHubImage(dockerConfiguration.getImage())
                    .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
            return withLinuxAppFramework.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
        }
    }

    public class WebAppUpdater extends AbstractAppServiceUpdater<WebApp> {
        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";
        private boolean modified = false;

        @Override
        public WebApp commit() {
            Update update = getWebAppInner().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (getAppSettings() != null && getAppSettings().isPresent()) {
                // todo: enhance app settings update, as now we could only add new app settings but can not remove existing values
                modified = true;
                update.withAppSettings(getAppSettings().get());
            }
            if (modified) {
                WebApp.this.webAppInner = update.apply();
            }
            WebApp.this.entity = AppServiceUtils.fromWebApp(WebApp.this.webAppInner);
            return WebApp.this;
        }

        private Update updateAppServicePlan(Update update, AppServicePlanEntity newServicePlan) {
            final AppServicePlanEntity currentServicePlan = azureAppService.appServicePlan(getWebAppInner().appServicePlanId()).entity();
            if (StringUtils.equalsIgnoreCase(currentServicePlan.getId(), newServicePlan.getId()) ||
                (StringUtils.equalsIgnoreCase(currentServicePlan.getName(), newServicePlan.getName()) &&
                    StringUtils.equalsIgnoreCase(currentServicePlan.getResourceGroup(), newServicePlan.getResourceGroup()))) {
                return update;
            }
            final AppServicePlan newPlanServiceModel = AppServiceUtils.getAppServicePlan(newServicePlan, azureClient);
            if (newPlanServiceModel == null) {
                throw new AzureToolkitRuntimeException("Target app service plan not exists");
            }
            modified = true;
            return update.withExistingAppServicePlan(newPlanServiceModel);
        }

        private Update updateRuntime(Update update, Runtime newRuntime) {
            final Runtime current = WebApp.this.getRuntime();
            if (Objects.equals(current, newRuntime)) {
                return update;
            }
            if (current.getOperatingSystem() != newRuntime.getOperatingSystem()) {
                throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
            }
            modified = true;
            switch (newRuntime.getOperatingSystem()) {
                case LINUX:
                    return update.withBuiltInImage(AppServiceUtils.toLinuxRuntimeStack(newRuntime));
                case WINDOWS:
                    return (Update) update.withJavaVersion(AppServiceUtils.toWindowsJavaVersion(newRuntime))
                        .withWebContainer(AppServiceUtils.toWindowsWebContainer(newRuntime));
                case DOCKER:
                    final DockerConfiguration dockerConfiguration = getDockerConfiguration().get();
                    if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                        return update.withPublicDockerHubImage(dockerConfiguration.getImage());
                    }
                    if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                        return update.withPrivateDockerHubImage(dockerConfiguration.getImage())
                            .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
                    }
                    return update.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
                default:
                    throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
            }
        }
    }
}
