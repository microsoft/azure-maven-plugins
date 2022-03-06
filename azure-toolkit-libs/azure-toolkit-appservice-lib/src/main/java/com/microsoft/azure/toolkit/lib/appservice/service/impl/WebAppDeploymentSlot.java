/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.WebDeploymentSlotBasic;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebAppDeploymentSlot extends AbstractAppService<DeploymentSlot, WebAppDeploymentSlotEntity> implements IWebAppBase<WebAppDeploymentSlotEntity> {
    private static final String WEB_APP_DEPLOYMENT_SLOT_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s/slots/%s";
    @Nonnull
    private final WebApp parent;
    @Nonnull
    private final com.azure.resourcemanager.appservice.models.WebApp webAppClient;

    public WebAppDeploymentSlot(@Nonnull final String id, @Nonnull final AppServiceManager azureClient) {
        super(id);
        this.webAppClient = azureClient.webApps().getById(ResourceId.fromString(id).parent().id());
        this.parent = Azure.az(AzureAppService.class).webapp(subscriptionId, resourceGroup, webAppClient.name());
    }

    public WebAppDeploymentSlot(@Nonnull final WebApp parent, @Nonnull final com.azure.resourcemanager.appservice.models.WebApp webAppClient,
                                @Nonnull final String slotName) {
        super(Utils.getSubscriptionId(webAppClient.id()), webAppClient.resourceGroupName(), slotName);
        this.webAppClient = webAppClient;
        this.parent = parent;
    }

    public WebAppDeploymentSlot(@Nonnull final WebApp parent, @Nonnull final com.azure.resourcemanager.appservice.models.WebApp webAppClient,
                                @Nonnull final WebDeploymentSlotBasic slotBasic) {
        super(slotBasic);
        this.webAppClient = webAppClient;
        this.parent = parent;
    }

    public WebApp webApp() {
        return parent;
    }

    public WebAppDeploymentSlotCreator create() {
        return new WebAppDeploymentSlotCreator();
    }

    public WebAppDeploymentSlotUpdater update() {
        return new WebAppDeploymentSlotUpdater();
    }

    @Nonnull
    @Override
    protected WebAppDeploymentSlotEntity getEntityFromRemoteResource(@Nonnull DeploymentSlot remote) {
        return AppServiceUtils.fromWebAppDeploymentSlot(remote);
    }

    @Override
    protected DeploymentSlot loadRemote() {
        return webAppClient.deploymentSlots().getByName(name);
    }

    @Override
    @AzureOperation(name = "webapp.delete_slot.slot|app", params = {"this.name(), this.parent.name()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        remote().parent().deploymentSlots().deleteById(this.id());
        webApp().refresh();
    }

    @Override
    @AzureOperation(name = "webapp.start_slot.slot|app", params = {"this.name(), this.parent.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        super.start();
    }

    @Override
    @AzureOperation(name = "webapp.stop_slot.slot|app", params = {"this.name(), this.parent.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        super.stop();
    }

    @Override
    @AzureOperation(name = "webapp.restart_slot.slot|app", params = {"this.name(), this.parent.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        super.restart();
    }

    @Override
    public void deploy(DeployType deployType, File targetFile, String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        remote().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public String id() {
        return String.format(WEB_APP_DEPLOYMENT_SLOT_ID_TEMPLATE, subscriptionId, resourceGroup, webAppClient.name(), name);
    }

    @Getter
    public class WebAppDeploymentSlotCreator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";
        private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Map<String, String> appSettings = null;
        private DiagnosticConfig diagnosticConfig = null;

        public WebAppDeploymentSlotCreator withName(String name) {
            this.name = name;
            return this;
        }

        public WebAppDeploymentSlotCreator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = appSettings;
            return this;
        }

        public WebAppDeploymentSlotCreator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        public WebAppDeploymentSlotCreator withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        public WebAppDeploymentSlot commit() {
            final com.azure.resourcemanager.appservice.models.WebApp webApp = WebAppDeploymentSlot.this.webAppClient;
            final DeploymentSlot.DefinitionStages.Blank blank = webApp.deploymentSlots().define(getName());
            final DeploymentSlot.DefinitionStages.WithCreate withCreate;
            // Using configuration from parent by default
            final String source = StringUtils.isEmpty(configurationSource) ? CONFIGURATION_SOURCE_PARENT : StringUtils.lowerCase(configurationSource);
            switch (source) {
                case CONFIGURATION_SOURCE_NEW:
                    withCreate = blank.withBrandNewConfiguration();
                    break;
                case CONFIGURATION_SOURCE_PARENT:
                    withCreate = blank.withConfigurationFromParent();
                    break;
                default:
                    try {
                        final DeploymentSlot deploymentSlot = Optional.ofNullable(webApp.deploymentSlots().getByName(configurationSource))
                                .orElseThrow(() -> new AzureToolkitRuntimeException(CONFIGURATION_SOURCE_DOES_NOT_EXISTS));
                        withCreate = blank.withConfigurationFromDeploymentSlot(deploymentSlot);
                    } catch (ManagementException e) {
                        throw new AzureToolkitRuntimeException(FAILED_TO_GET_CONFIGURATION_SOURCE, e);
                    }
                    break;
            }
            if (getAppSettings() != null) {
                withCreate.withAppSettings(getAppSettings());
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, getDiagnosticConfig());
            }
            WebAppDeploymentSlot.this.remote = withCreate.create();
            WebAppDeploymentSlot.this.entity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.remote);
            WebAppDeploymentSlot.this.refreshStatus();
            WebAppDeploymentSlot.this.parent.refresh();
            return WebAppDeploymentSlot.this;
        }
    }

    @Getter
    public class WebAppDeploymentSlotUpdater {
        private final List<String> appSettingsToRemove = new ArrayList<>();
        private final Map<String, String> appSettingsToAdd = new HashMap<>();
        private DiagnosticConfig diagnosticConfig = null;

        public WebAppDeploymentSlotUpdater withoutAppSettings(String key) {
            this.appSettingsToRemove.add(key);
            return this;
        }

        public WebAppDeploymentSlotUpdater withAppSettings(Map<String, String> appSettings) {
            this.appSettingsToAdd.putAll(appSettings);
            return this;
        }

        public WebAppDeploymentSlotUpdater withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        public WebAppDeploymentSlot commit() {
            final DeploymentSlotBase.Update<DeploymentSlot> update = remote().update();
            if (getAppSettingsToAdd() != null) {
                update.withAppSettings(getAppSettingsToAdd());
            }
            if (getAppSettingsToRemove() != null) {
                getAppSettingsToRemove().forEach(update::withoutAppSetting);
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, getDiagnosticConfig());
            }
            WebAppDeploymentSlot.this.remote = update.apply();
            WebAppDeploymentSlot.this.entity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.remote);
            WebAppDeploymentSlot.this.refreshStatus();
            return WebAppDeploymentSlot.this;
        }
    }
}
