/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WebApp extends AbstractAppService<com.azure.resourcemanager.appservice.models.WebApp, WebAppEntity> implements IWebApp {
    private static final ClientLogger LOGGER = new ClientLogger(WebApp.class);
    private static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";

    private final AzureResourceManager azureClient;

    public WebApp(WebAppEntity entity, AzureResourceManager azureClient) {
        this.entity = entity;
        this.azureClient = azureClient;
    }

    @Override
    public IAppServicePlan plan() {
        return Azure.az(AzureAppService.class).appServicePlan(getRemoteResource().appServicePlanId());
    }

    @Override
    public WebAppCreator create() {
        return new WebAppCreator();
    }

    @NotNull
    @Override
    protected WebAppEntity getEntityFromRemoteResource(@NotNull com.azure.resourcemanager.appservice.models.WebApp remote) {
        return AppServiceUtils.fromWebApp(remote);
    }

    @Override
    protected com.azure.resourcemanager.appservice.models.WebApp remote() {
        return StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.webApps().getById(entity.getId()) :
                azureClient.webApps().getByResourceGroup(entity.getResourceGroup(), entity.getName());
    }

    @Override
    public void delete() {
        azureClient.webApps().deleteById(getRemoteResource().id());
    }

    @Override
    public void deploy(@Nonnull DeployType deployType, @Nonnull File targetFile, @Nullable String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        LOGGER.info(String.format("Deploying (%s)[%s] %s ...", TextUtils.cyan(targetFile.toString()),
                TextUtils.cyan(deployType.toString()),
                StringUtils.isBlank(targetPath) ? "" : (" to " + TextUtils.green(targetPath))));
        getRemoteResource().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public WebAppUpdater update() {
        return new WebAppUpdater();
    }

    @Override
    @Cacheable(cacheName = "appservice/webapp/{}/slot/{}", key = "${this.name()}/$slotName")
    public IWebAppDeploymentSlot deploymentSlot(String slotName) {
        final WebAppDeploymentSlotEntity slotEntity = WebAppDeploymentSlotEntity.builder().name(slotName)
            .resourceGroup(getRemoteResource().resourceGroupName())
            .webappName(getRemoteResource().name()).build();
        return new WebAppDeploymentSlot(slotEntity, azureClient);
    }

    @Override
    @Cacheable(cacheName = "appservice/webapp/{}/slots", key = "${this.name()}", condition = "!(force&&force[0])")
    public List<IWebAppDeploymentSlot> deploymentSlots(boolean... force) {
        return getRemoteResource().deploymentSlots().list().stream()
            .map(slot -> new WebAppDeploymentSlot(WebAppDeploymentSlotEntity.builder().id(slot.id()).build(), azureClient))
            .collect(Collectors.toList());
    }

    @Override
    public void swap(String slotName) {
        getRemoteResource().swap(slotName);
    }

    public class WebAppCreator extends AbstractAppServiceCreator<WebApp> {
        // todo: Add validation for required parameters
        @Override
        public WebApp commit() {
            final DefinitionStages.Blank blank = WebApp.this.azureClient.webApps().define(getName());
            final Runtime runtime = getRuntime();
            final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlan(getAppServicePlanEntity(), azureClient);
            if (appServicePlan == null) {
                throw new AzureToolkitRuntimeException("Target app service plan not exists");
            }
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
            if (getDiagnosticConfig() != null && getDiagnosticConfig().isPresent()) {
                AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, getDiagnosticConfig().get());
            }
            WebApp.this.remote = withCreate.create();
            WebApp.this.entity = AppServiceUtils.fromWebApp(WebApp.this.remote);
            return WebApp.this;
        }

        DefinitionStages.WithCreate createWindowsWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                        Runtime runtime) {
            return (DefinitionStages.WithCreate) blank.withExistingWindowsPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withJavaVersion(AppServiceUtils.toJavaVersion(runtime.getJavaVersion()))
                .withWebContainer(AppServiceUtils.toWebContainer(runtime));
        }

        DefinitionStages.WithCreate createLinuxWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                      Runtime runtime) {
            return blank.withExistingLinuxPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withBuiltInImage(AppServiceUtils.toRuntimeStack(runtime));
        }

        DefinitionStages.WithCreate createDockerWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                       DockerConfiguration dockerConfiguration) {
            final DefinitionStages.WithLinuxAppFramework withLinuxAppFramework =
                    blank.withExistingLinuxPlan(appServicePlan).withExistingResourceGroup(resourceGroup);
            final DefinitionStages.WithStartUpCommand withStartUpCommand;
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                withStartUpCommand = withLinuxAppFramework.withPublicDockerHubImage(dockerConfiguration.getImage());
            } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                withStartUpCommand = withLinuxAppFramework.withPrivateDockerHubImage(dockerConfiguration.getImage())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            } else {

                withStartUpCommand = withLinuxAppFramework.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
            return withStartUpCommand.withStartUpCommand(dockerConfiguration.getStartUpCommand());
        }
    }

    public class WebAppUpdater extends AbstractAppServiceUpdater<WebApp> {
        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";
        private boolean modified = false;

        @Override
        public WebApp commit() {
            Update update = getRemoteResource().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (!Collections.isEmpty(getAppSettingsToAdd())) {
                modified = true;
                update.withAppSettings(getAppSettingsToAdd());
            }
            if (!Collections.isEmpty(getAppSettingsToRemove())) {
                modified = true;
                getAppSettingsToRemove().forEach(update::withoutAppSetting);
            }
            if (getDiagnosticConfig() != null && getDiagnosticConfig().isPresent()) {
                modified = true;
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, getDiagnosticConfig().get());
            }
            if (modified) {
                WebApp.this.remote = update.apply();
            }
            WebApp.this.entity = AppServiceUtils.fromWebApp(WebApp.this.remote);
            return WebApp.this;
        }

        private Update updateAppServicePlan(Update update, AppServicePlanEntity newServicePlan) {
            final String servicePlanId = getRemoteResource().appServicePlanId();
            final AppServicePlanEntity currentServicePlan = Azure.az(AzureAppService.class).appServicePlan(servicePlanId).entity();
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
            if (newRuntime.getOperatingSystem() != null && current.getOperatingSystem() != newRuntime.getOperatingSystem()) {
                throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
            }
            modified = true;
            final OperatingSystem operatingSystem = ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), current.getOperatingSystem());
            switch (operatingSystem) {
                case LINUX:
                    return update.withBuiltInImage(AppServiceUtils.toRuntimeStack(newRuntime));
                case WINDOWS:
                    return (Update) update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                        .withWebContainer(AppServiceUtils.toWebContainer(newRuntime));
                case DOCKER:
                    final DockerConfiguration dockerConfiguration = getDockerConfiguration().get();
                    final com.azure.resourcemanager.appservice.models.WebApp.UpdateStages.WithStartUpCommand withStartUpCommand;
                    if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                        withStartUpCommand = update.withPublicDockerHubImage(dockerConfiguration.getImage());
                    } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                        withStartUpCommand = update.withPrivateDockerHubImage(dockerConfiguration.getImage())
                                .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
                    } else {
                        withStartUpCommand = update.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                                .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
                    }
                    return withStartUpCommand.withStartUpCommand(dockerConfiguration.getStartUpCommand());
                default:
                    throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
            }
        }
    }
}
