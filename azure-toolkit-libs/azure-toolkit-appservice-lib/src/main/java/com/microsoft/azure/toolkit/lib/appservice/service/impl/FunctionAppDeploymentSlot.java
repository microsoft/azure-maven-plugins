/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.fluent.models.HostKeysInner;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlotBasic;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FunctionAppDeploymentSlot extends FunctionAppBase<FunctionDeploymentSlot, FunctionAppDeploymentSlotEntity>
        implements IFunctionAppDeploymentSlot {
    private static final String FUNCTION_DEPLOYMENT_SLOT_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s/slots/%s";

    @Nonnull
    private final FunctionApp parent;

    public FunctionAppDeploymentSlot(@Nonnull final FunctionApp functionApp, @Nonnull final String slotName) {
        super(Utils.getSubscriptionId(functionApp.id()), functionApp.resourceGroupName(), slotName);
        this.parent = functionApp;
    }

    public FunctionAppDeploymentSlot(@Nonnull final FunctionApp functionApp, @Nonnull final FunctionDeploymentSlotBasic slotBasic) {
        super(slotBasic);
        this.parent = functionApp;
    }

    @Override
    public IFunctionApp functionApp() {
        return Azure.az(AzureAppService.class).functionApp(resourceGroup, parent.name());
    }

    @Override
    public Creator create() {
        return new FunctionAppDeploymentSlotCreator();
    }

    @Override
    public Updater update() {
        return new FunctionAppDeploymentSlotUpdater();
    }

    @NotNull
    @Override
    protected FunctionAppDeploymentSlotEntity getEntityFromRemoteResource(@NotNull FunctionDeploymentSlot remote) {
        return AppServiceUtils.fromFunctionAppDeploymentSlot(remote);
    }

    @Override
    public void delete() {
        parent.deploymentSlots().deleteById(this.id());
    }

    @Nullable
    @Override
    protected FunctionDeploymentSlot loadRemote() {
        return parent.deploymentSlots().getByName(name);
    }

    @Override
    public String getMasterKey() {
        final String resourceGroup = entity().getResourceGroup();
        final String name = String.format("%s/slots/%s", entity().getFunctionAppName(), entity().getName());
        return remote().manager().serviceClient().getWebApps().listHostKeysAsync(resourceGroup, name).map(HostKeysInner::masterKey).block();
    }

    @Override
    public String id() {
        return String.format(FUNCTION_DEPLOYMENT_SLOT_ID_TEMPLATE, subscriptionId, resourceGroup, parent.name(), name);
    }

    @Getter
    public class FunctionAppDeploymentSlotCreator implements IFunctionAppDeploymentSlot.Creator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";
        private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Map<String, String> appSettings = null;
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public IFunctionAppDeploymentSlot.Creator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public IFunctionAppDeploymentSlot.Creator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = appSettings;
            return this;
        }

        @Override
        public IFunctionAppDeploymentSlot.Creator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        @Override
        public IFunctionAppDeploymentSlot.Creator withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public IFunctionAppDeploymentSlot commit() {
            final FunctionApp functionApp = parent;
            final FunctionDeploymentSlot.DefinitionStages.Blank blank = functionApp.deploymentSlots().define(getName());
            final FunctionDeploymentSlot.DefinitionStages.WithCreate withCreate;
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
                        final FunctionDeploymentSlot deploymentSlot = Optional.ofNullable(functionApp.deploymentSlots().getByName(configurationSource))
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
            FunctionAppDeploymentSlot.this.remote = withCreate.create();
            FunctionAppDeploymentSlot.this.entity = AppServiceUtils.fromFunctionAppDeploymentSlot(FunctionAppDeploymentSlot.this.remote);
            return FunctionAppDeploymentSlot.this;
        }
    }

    @Getter
    private class FunctionAppDeploymentSlotUpdater implements IFunctionAppDeploymentSlot.Updater {
        private final List<String> appSettingsToRemove = new ArrayList<>();
        private final Map<String, String> appSettingsToAdd = new HashMap<>();
        private DiagnosticConfig diagnosticConfig = null;

        @Override
        public Updater withoutAppSettings(String key) {
            appSettingsToRemove.add(key);
            return this;
        }

        public IFunctionAppDeploymentSlot.Updater withAppSettings(Map<String, String> appSettings) {
            appSettingsToAdd.putAll(appSettings);
            return this;
        }

        @Override
        public IFunctionAppDeploymentSlot.Updater withDiagnosticConfig(DiagnosticConfig diagnosticConfig) {
            this.diagnosticConfig = diagnosticConfig;
            return this;
        }

        @Override
        public FunctionAppDeploymentSlot commit() {
            final DeploymentSlotBase.Update<FunctionDeploymentSlot> update = remote().update();
            if (getAppSettingsToAdd() != null) {
                update.withAppSettings(getAppSettingsToAdd());
            }
            if (getAppSettingsToRemove() != null) {
                getAppSettingsToRemove().forEach(update::withoutAppSetting);
            }
            if (getDiagnosticConfig() != null) {
                AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, getDiagnosticConfig());
            }
            FunctionAppDeploymentSlot.this.remote = update.apply();
            FunctionAppDeploymentSlot.this.entity = AppServiceUtils.fromFunctionAppDeploymentSlot(FunctionAppDeploymentSlot.this.remote);
            return FunctionAppDeploymentSlot.this;
        }
    }
}
