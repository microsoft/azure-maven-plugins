/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class WebApp extends AbstractAppService<com.azure.resourcemanager.appservice.models.WebApp, WebAppEntity> implements IWebAppBase<WebAppEntity> {
    private static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";

    private final AppServiceManager azureClient;

    public WebApp(@Nonnull final String id, @Nonnull final AppServiceManager azureClient) {
        super(id);
        this.azureClient = azureClient;
    }

    public WebApp(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name,
                  @Nonnull final AppServiceManager azureClient) {
        super(subscriptionId, resourceGroup, name);
        this.azureClient = azureClient;
    }

    public WebApp(@Nonnull WebSiteBase webAppBasic, @Nonnull final AppServiceManager azureClient) {
        super(webAppBasic);
        this.azureClient = azureClient;
    }

    public com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan plan() {
        return Azure.az(AzureAppServicePlan.class).get(remote().appServicePlanId());
    }

    public WebAppCreator create() {
        return new WebAppCreator();
    }

    @Nonnull
    @Override
    protected WebAppEntity getEntityFromRemoteResource(@Nonnull com.azure.resourcemanager.appservice.models.WebApp remote) {
        return AppServiceUtils.fromWebApp(remote);
    }

    @Override
    protected com.azure.resourcemanager.appservice.models.WebApp loadRemote() {
        return azureClient.webApps().getByResourceGroup(resourceGroup, name);
    }

    @Override
    @AzureOperation(name = "webapp.delete_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (this.exists()) {
            this.status(Status.PENDING);
            azureClient.functionApps().deleteById(this.id());
            Azure.az(AzureWebApp.class).refresh();
        }
    }

    @Override
    @AzureOperation(name = "webapp.start_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        super.start();
    }

    @Override
    @AzureOperation(name = "webapp.stop_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        super.stop();
    }

    @Override
    @AzureOperation(name = "webapp.restart_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        super.restart();
    }

    @Override
    public void deploy(@Nonnull DeployType deployType, @Nonnull File targetFile, @Nullable String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        AzureMessager.getMessager().info(AzureString.format("Deploying (%s)[%s] %s ...", targetFile.toString(),
            (deployType.toString()),
            StringUtils.isBlank(targetPath) ? "" : (" to " + (targetPath))));
        remote().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    public WebAppUpdater update() {
        return new WebAppUpdater();
    }

    @Override
    public void refresh() {
        try {
            super.refresh();
        } finally {
            try {
                CacheManager.evictCache("appservice/webapp/{}/slots", this.name());
                CacheManager.evictCache("appservice/webapp/{}/slot/{}", CacheEvict.ALL);
            } catch (Throwable e) {
                log.warn("failed to evict cache", e);
            }
        }
    }

    @Cacheable(cacheName = "appservice/webapp/{}/slot/{}", key = "${this.name()}/$slotName")
    public WebAppDeploymentSlot deploymentSlot(String slotName) {
        return new WebAppDeploymentSlot(this, remote(), slotName);
    }

    @Cacheable(cacheName = "appservice/webapp/{}/slots", key = "${this.name()}", condition = "!(force&&force[0])")
    public List<WebAppDeploymentSlot> deploymentSlots(boolean... force) {
        return remote().deploymentSlots().list().stream().map(slot -> deploymentSlot(slot.name())).collect(Collectors.toList());
    }

    public void swap(String slotName) {
        remote().swap(slotName);
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
            final DefinitionStages.WithCreate withCreate;
            switch (runtime.getOperatingSystem()) {
                case LINUX:
                    withCreate = createLinuxWebApp(blank, appServicePlan, runtime);
                    break;
                case WINDOWS:
                    withCreate = createWindowsWebApp(blank, appServicePlan, runtime);
                    break;
                case DOCKER:
                    final DockerConfiguration dockerConfiguration = getDockerConfiguration().get();
                    withCreate = createDockerWebApp(blank, appServicePlan, dockerConfiguration);
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
            WebApp.this.refreshStatus();
            Azure.az(AzureWebApp.class).refresh();
            return WebApp.this;
        }

        DefinitionStages.WithCreate createWindowsWebApp(DefinitionStages.Blank blank, AppServicePlan appServicePlan, Runtime runtime) {
            return (DefinitionStages.WithCreate) blank.withExistingWindowsPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withJavaVersion(AppServiceUtils.toJavaVersion(runtime.getJavaVersion()))
                .withWebContainer(AppServiceUtils.toWebContainer(runtime));
        }

        DefinitionStages.WithCreate createLinuxWebApp(DefinitionStages.Blank blank, AppServicePlan appServicePlan, Runtime runtime) {
            return blank.withExistingLinuxPlan(appServicePlan)
                .withExistingResourceGroup(resourceGroup)
                .withBuiltInImage(AppServiceUtils.toRuntimeStack(runtime));
        }

        DefinitionStages.WithCreate createDockerWebApp(DefinitionStages.Blank blank, AppServicePlan appServicePlan, DockerConfiguration dockerConfiguration) {
            final DefinitionStages.WithLinuxAppFramework withLinuxAppFramework =
                blank.withExistingLinuxPlan(appServicePlan).withExistingResourceGroup(resourceGroup);
            final DefinitionStages.WithStartUpCommand draft;
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                draft = withLinuxAppFramework.withPublicDockerHubImage(dockerConfiguration.getImage());
            } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                draft = withLinuxAppFramework.withPrivateDockerHubImage(dockerConfiguration.getImage())
                    .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            } else {
                draft = withLinuxAppFramework.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                    .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
            return draft.withStartUpCommand(dockerConfiguration.getStartUpCommand());
        }
    }

    public class WebAppUpdater extends AbstractAppServiceUpdater<WebApp> {
        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";
        private boolean modified = false;

        @Override
        public WebApp commit() {
            Update update = remote().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (getDockerConfiguration() != null && getDockerConfiguration().isPresent() && WebApp.this.getRuntime().isDocker()) {
                modified = true;
                update = updateDockerConfiguration(update, getDockerConfiguration().get());
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
            Azure.az(AzureWebApp.class).refresh(); // todo: refactor to support refresh single subscription
            WebApp.this.refreshStatus();
            return WebApp.this;
        }

        private Update updateAppServicePlan(Update update, AppServicePlanEntity newServicePlan) {
            final com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan appServicePlan = WebApp.this.getAppServicePlan();
            if (StringUtils.equalsIgnoreCase(appServicePlan.getId(), newServicePlan.getId()) ||
                (StringUtils.equalsIgnoreCase(appServicePlan.getName(), newServicePlan.getName()) &&
                    StringUtils.equalsIgnoreCase(appServicePlan.getResourceGroupName(), newServicePlan.getResourceGroup()))) {
                return update;
            }
            final AppServicePlan newPlanServiceModel = AppServiceUtils.getAppServicePlan(newServicePlan, azureClient);
            if (newPlanServiceModel == null) {
                throw new AzureToolkitRuntimeException("Target app service plan not exists");
            }
            modified = true;
            return update.withExistingAppServicePlan(newPlanServiceModel);
        }

        private Update updateDockerConfiguration(Update update, DockerConfiguration dockerConfiguration) {
            final com.azure.resourcemanager.appservice.models.WebApp.UpdateStages.WithStartUpCommand draft;
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                draft = update.withPublicDockerHubImage(dockerConfiguration.getImage());
            } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                draft = update.withPrivateDockerHubImage(dockerConfiguration.getImage())
                    .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            } else {
                draft = update.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                    .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
            modified = true;
            return draft.withStartUpCommand(dockerConfiguration.getStartUpCommand());
        }

        private Update updateRuntime(Update update, Runtime newRuntime) {
            final Runtime current = WebApp.this.getRuntime();
            if (newRuntime.getOperatingSystem() != null && current.getOperatingSystem() != newRuntime.getOperatingSystem()) {
                throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
            }
            if (Objects.equals(current, newRuntime) || current.isDocker()) {
                return update;
            }
            modified = true;
            final OperatingSystem operatingSystem = ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), current.getOperatingSystem());
            switch (operatingSystem) {
                case LINUX:
                    return update.withBuiltInImage(AppServiceUtils.toRuntimeStack(newRuntime));
                case WINDOWS:
                    return (Update) update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                        .withWebContainer(AppServiceUtils.toWebContainer(newRuntime));
                default:
                    throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
            }
        }
    }
}
