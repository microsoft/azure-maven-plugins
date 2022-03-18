/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class FunctionAppDeploymentSlotDraft extends FunctionAppDeploymentSlot
    implements AzResource.Draft<FunctionAppDeploymentSlot, FunctionDeploymentSlot> {
    public static final String CONFIGURATION_SOURCE_NEW = "new";
    public static final String CONFIGURATION_SOURCE_PARENT = "parent";
    private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current app";
    private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

    @Getter
    @Nullable
    private final FunctionAppDeploymentSlot origin;
    @Nullable
    private Config config;

    protected FunctionAppDeploymentSlotDraft(@Nonnull String name, @Nonnull FunctionAppDeploymentSlotModule module) {
        super(name, module);
        this.origin = null;
    }

    protected FunctionAppDeploymentSlotDraft(@Nonnull FunctionAppDeploymentSlot origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public FunctionDeploymentSlot createResourceInAzure() {
        final String name = getName();
        final Map<String, String> newAppSettings = this.getAppSettings();
        final DiagnosticConfig newDiagnosticConfig = this.getDiagnosticConfig();
        final String newConfigurationSource = this.getConfigurationSource();
        // Using configuration from parent by default
        final String source = StringUtils.isBlank(newConfigurationSource) ? CONFIGURATION_SOURCE_PARENT : StringUtils.lowerCase(newConfigurationSource);

        final FunctionApp functionApp = Objects.requireNonNull(this.getParent().getRemote());
        final FunctionDeploymentSlot.DefinitionStages.Blank blank = functionApp.deploymentSlots().define(getName());
        final FunctionDeploymentSlot.DefinitionStages.WithCreate withCreate;
        if (CONFIGURATION_SOURCE_NEW.equals(source)) {
            withCreate = blank.withBrandNewConfiguration();
        } else if (CONFIGURATION_SOURCE_PARENT.equals(source)) {
            withCreate = blank.withConfigurationFromParent();
        } else {
            try {
                final FunctionDeploymentSlot sourceSlot = functionApp.deploymentSlots().getByName(newConfigurationSource);
                Objects.requireNonNull(sourceSlot, CONFIGURATION_SOURCE_DOES_NOT_EXISTS);
                withCreate = blank.withConfigurationFromDeploymentSlot(sourceSlot);
            } catch (ManagementException e) {
                throw new AzureToolkitRuntimeException(FAILED_TO_GET_CONFIGURATION_SOURCE, e);
            }
        }
        if (MapUtils.isNotEmpty(newAppSettings)) {
            // todo: support remove app settings
            withCreate.withAppSettings(newAppSettings);
        }
        if (Objects.nonNull(newDiagnosticConfig)) {
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig);
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Azure Functions app deployment slot ({0})...", name));
        FunctionDeploymentSlot slot = Objects.requireNonNull(this.doModify(() -> withCreate.create(), Status.CREATING));
        messager.success(AzureString.format("Azure Functions app deployment slot ({0}) is successfully created", name));
        return slot;
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public FunctionDeploymentSlot updateResourceInAzure(@Nonnull FunctionDeploymentSlot remote) {
        final Map<String, String> settingsToAdd = this.getAppSettings();
        final Set<String> settingsToRemove = this.getAppSettingsToRemove();
        final DiagnosticConfig newDiagnosticConfig = this.getDiagnosticConfig();

        boolean modified = MapUtils.isNotEmpty(settingsToAdd) || CollectionUtils.isNotEmpty(settingsToRemove) || Objects.nonNull(newDiagnosticConfig);

        if (modified) {
            final DeploymentSlotBase.Update<FunctionDeploymentSlot> update = remote.update();
            Optional.ofNullable(settingsToAdd).ifPresent(update::withAppSettings);
            Optional.ofNullable(settingsToRemove).ifPresent(s -> s.forEach(update::withoutAppSetting));
            Optional.ofNullable(newDiagnosticConfig).ifPresent(c -> AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, newDiagnosticConfig));

            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Azure Functions app deployment slot({0})...", remote.name()));
            remote = update.apply();
            messager.success(AzureString.format("Azure Functions app deployment slot({0}) is successfully updated", remote.name()));
        }
        return remote;
    }

    public void setConfigurationSource(String source) {
        this.ensureConfig().setConfigurationSource(source);
    }

    @Nullable
    public String getConfigurationSource() {
        return Optional.ofNullable(config).map(Config::getConfigurationSource).orElse(null);
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.ensureConfig().setAppSettings(appSettings);
    }

    @Nullable
    @Override
    public Map<String, String> getAppSettings() {
        return Optional.ofNullable(config).map(Config::getAppSettings).orElseGet(super::getAppSettings);
    }

    public void removeAppSetting(String key) {
        this.ensureConfig().getAppSettingsToRemove().add(key);
    }

    @Nullable
    public Set<String> getAppSettingsToRemove() {
        return Optional.ofNullable(config).map(Config::getAppSettingsToRemove).orElse(new HashSet<>());
    }

    public void setDiagnosticConfig(DiagnosticConfig config) {
        this.ensureConfig().setDiagnosticConfig(config);
    }

    @Nullable
    public DiagnosticConfig getDiagnosticConfig() {
        return Optional.ofNullable(config).map(Config::getDiagnosticConfig).orElse(null);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            StringUtils.isBlank(this.config.getConfigurationSource()) ||
            Objects.isNull(this.config.getDiagnosticConfig()) ||
            CollectionUtils.isEmpty(this.config.getAppSettingsToRemove()) ||
            Objects.isNull(this.config.getAppSettings()) || Objects.equals(this.config.getAppSettings(), super.getAppSettings());
        return !notModified;
    }

    @Data
    @Nullable
    private static class Config {
        private String configurationSource;
        private DiagnosticConfig diagnosticConfig = null;
        private Set<String> appSettingsToRemove = new HashSet<>();
        private Map<String, String> appSettings = null;
    }
}