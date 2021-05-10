/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlotCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlotUpdater;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class WebAppDeploymentSlot extends AbstractAppService<DeploymentSlot> implements IWebAppDeploymentSlot {

    private WebAppDeploymentSlotEntity slotEntity;

    private DeploymentSlot deploymentSlotInner;
    private final AzureResourceManager azureClient;

    public WebAppDeploymentSlot(WebAppDeploymentSlotEntity deploymentSlot, AzureResourceManager azureClient) {
        this.slotEntity = deploymentSlot;
        this.azureClient = azureClient;
    }

    @Override
    public IWebApp webApp() {
        final WebAppDeploymentSlotEntity entity = entity();
        return Azure.az(AzureAppService.class).webapp(entity.getSubscriptionId(), entity.getResourceGroup(), entity.getWebappName());
    }

    @Override
    public IWebAppDeploymentSlotCreator create() {
        return new WebAppDeploymentSlotCreator();
    }

    @Override
    public IWebAppDeploymentSlotUpdater update() {
        return new WebAppDeploymentSlotUpdater();
    }

    @Override
    protected DeploymentSlot getResourceInner() {
        final WebApp parentWebApp = getParentWebApp();
        if (deploymentSlotInner == null) {
            deploymentSlotInner = StringUtils.isNotEmpty(slotEntity.getId()) ? parentWebApp.deploymentSlots().getById(slotEntity.getId()) :
                    parentWebApp.deploymentSlots().getByName(slotEntity.getName());
            slotEntity = AppServiceUtils.fromWebAppDeploymentSlot(deploymentSlotInner);
        }
        return deploymentSlotInner;
    }

    @Override
    public void delete() {
        getNonnullResourceInner().parent().deploymentSlots().deleteByName(slotEntity.getName());
    }

    @Override
    public void deploy(DeployType deployType, File targetFile, String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        getNonnullResourceInner().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public WebAppDeploymentSlotEntity entity() {
        return slotEntity;
    }

    private WebApp getParentWebApp() {
        return StringUtils.isNotEmpty(slotEntity.getId()) ?
                azureClient.webApps().getById(slotEntity.getId().substring(0, slotEntity.getId().indexOf("/slots"))) :
                azureClient.webApps().getByResourceGroup(slotEntity.getResourceGroup(), slotEntity.getWebappName());
    }

    @Getter
    public class WebAppDeploymentSlotCreator implements IWebAppDeploymentSlotCreator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";
        private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Map<String, String> appSettings = null;
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public IWebAppDeploymentSlotCreator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = appSettings;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final WebApp webApp = getParentWebApp();
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
            WebAppDeploymentSlot.this.deploymentSlotInner = withCreate.create();
            WebAppDeploymentSlot.this.slotEntity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.deploymentSlotInner);
            return WebAppDeploymentSlot.this;
        }
    }

    @Getter
    private class WebAppDeploymentSlotUpdater implements IWebAppDeploymentSlotUpdater {
        private Map<String, String> appSettings = null;
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public WebAppDeploymentSlotUpdater withAppSettings(Map<String, String> appSettings) {
            this.appSettings = appSettings;
            return this;
        }

        @Override
        public WebAppDeploymentSlotUpdater withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final DeploymentSlotBase.Update<DeploymentSlot> update = getNonnullResourceInner().update();
            if (getAppSettings() != null) {
                update.withAppSettings(getAppSettings());
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, getDiagnosticConfig());
            }
            WebAppDeploymentSlot.this.deploymentSlotInner = update.apply();
            WebAppDeploymentSlot.this.slotEntity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.deploymentSlotInner);
            return WebAppDeploymentSlot.this;
        }
    }
}
