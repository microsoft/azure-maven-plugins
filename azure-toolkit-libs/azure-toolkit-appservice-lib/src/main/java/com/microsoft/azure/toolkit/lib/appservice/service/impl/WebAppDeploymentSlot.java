/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlotCreator;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class WebAppDeploymentSlot implements IWebAppDeploymentSlot {

    private AzureAppService azureAppService;
    private WebAppDeploymentSlotEntity slotEntity;

    private DeploymentSlot deploymentSlotInner;
    private AzureResourceManager azureClient;

    public WebAppDeploymentSlot(WebAppDeploymentSlotEntity deploymentSlot, AzureAppService azureAppService) {
        this.slotEntity = deploymentSlot;
        this.azureAppService = azureAppService;
        this.azureClient = azureAppService.getAzureResourceManager();
    }

    @Override
    public IWebApp webApp() {
        final WebAppDeploymentSlotEntity entity = entity();
        return azureAppService.webapp(entity.getResourceGroup(), entity.getWebappName());
    }

    @Override
    public IWebAppDeploymentSlotCreator create() {
        return new WebAppDeploymentSlotCreator();
    }

    @Override
    public void start() {
        getDeploymentSlotInner().start();
    }

    @Override
    public void stop() {
        getDeploymentSlotInner().stop();
    }

    @Override
    public void restart() {
        getDeploymentSlotInner().restart();
    }

    @Override
    public void delete() {
        getDeploymentSlotInner().parent().deploymentSlots().deleteByName(slotEntity.getName());
    }

    @Override
    public void deploy(DeployType deployType, File targetFile, String targetPath) {
        final DeployOptions options = new DeployOptions().withPath(targetPath);
        getDeploymentSlotInner().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), targetFile, options);
    }

    @Override
    public boolean exists() {
        refreshDeploymentSlotInner();
        return deploymentSlotInner != null;
    }

    @Override
    public String hostName() {
        return getDeploymentSlotInner().defaultHostname();
    }

    @Override
    public String state() {
        return getDeploymentSlotInner().state();
    }

    @Override
    public Runtime getRuntime() {
        return AppServiceUtils.getRuntimeFromWebApp(getDeploymentSlotInner());
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return AppServiceUtils.fromPublishingProfile(getDeploymentSlotInner().getPublishingProfile());
    }

    @Override
    public WebAppDeploymentSlotEntity entity() {
        return slotEntity;
    }

    private com.azure.resourcemanager.appservice.models.DeploymentSlot getDeploymentSlotInner() {
        if (deploymentSlotInner == null) {
            refreshDeploymentSlotInner();
        }
        return deploymentSlotInner;
    }

    private synchronized void refreshDeploymentSlotInner() {
        try {
            final WebApp webAppService = StringUtils.isNotEmpty(slotEntity.getId()) ?
                azureClient.webApps().getById(slotEntity.getId().substring(0, slotEntity.getId().indexOf("/slots"))) :
                azureClient.webApps().getByResourceGroup(slotEntity.getResourceGroup(), slotEntity.getWebappName());
            deploymentSlotInner = StringUtils.isNotEmpty(slotEntity.getId()) ? webAppService.deploymentSlots().getById(slotEntity.getId()) :
                webAppService.deploymentSlots().getByName(slotEntity.getName());
            slotEntity = AppServiceUtils.fromWebAppDeploymentSlot(deploymentSlotInner);
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            deploymentSlotInner = null;
        }
    }

    @Override
    public String id() {
        return getDeploymentSlotInner().id();
    }

    @Override
    public String name() {
        return getDeploymentSlotInner().name();
    }

    @Getter
    public class WebAppDeploymentSlotCreator implements IWebAppDeploymentSlotCreator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Optional<Map<String, String>> appSettings = null;
        private Optional<DiagnosticConfig> diagnosticConfig = null;

        @Override
        public IWebAppDeploymentSlotCreator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = Optional.ofNullable(appSettings);
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = Optional.ofNullable(diagnosticConfig);
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final WebAppDeploymentSlotEntity entity = WebAppDeploymentSlot.this.entity();
            final WebApp webApp = azureClient.webApps().getByResourceGroup(entity.getResourceGroup(), entity.getWebappName());
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
                    final DeploymentSlot deploymentSlot = deploymentSlotInner.parent().deploymentSlots().getByName(configurationSource);
                    if (deploymentSlot == null) {
                        throw new AzureToolkitRuntimeException(CONFIGURATION_SOURCE_DOES_NOT_EXISTS);
                    }
                    withCreate = blank.withConfigurationFromDeploymentSlot(deploymentSlot);
                    break;
            }
            if (appSettings != null && appSettings.isPresent()) {
                withCreate.withAppSettings(appSettings.get());
            }
            if (getDiagnosticConfig() != null && getDiagnosticConfig().isPresent()) {
                AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, getDiagnosticConfig().get());
            }
            WebAppDeploymentSlot.this.deploymentSlotInner = withCreate.create();
            WebAppDeploymentSlot.this.slotEntity = AppServiceUtils.fromWebAppDeploymentSlot(WebAppDeploymentSlot.this.deploymentSlotInner);
            return WebAppDeploymentSlot.this;
        }
    }
}
