/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlotBase;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft.CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS;
import static com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft.UNSUPPORTED_OPERATING_SYSTEM;

@Slf4j
public class WebAppDeploymentSlotDraft extends WebAppDeploymentSlot implements AzResource.Draft<WebAppDeploymentSlot, WebSiteBase> {
    private static final String CREATE_NEW_DEPLOYMENT_SLOT = "createNewDeploymentSlot";
    public static final String CONFIGURATION_SOURCE_NEW = "new";
    public static final String CONFIGURATION_SOURCE_PARENT = "parent";
    private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";
    private static final String FAILED_TO_GET_CONFIGURATION_SOURCE = "Failed to get configuration source slot";

    @Getter
    @Nullable
    private final WebAppDeploymentSlot origin;
    @Nullable
    private Config config;

    protected WebAppDeploymentSlotDraft(@Nonnull String name, @Nonnull WebAppDeploymentSlotModule module) {
        super(name, module);
        this.origin = null;
    }

    protected WebAppDeploymentSlotDraft(@Nonnull WebAppDeploymentSlot origin) {
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
    public DeploymentSlot createResourceInAzure() {
        OperationContext.action().setTelemetryProperty(CREATE_NEW_DEPLOYMENT_SLOT, String.valueOf(true));

        final String name = getName();
        final Map<String, String> newAppSettings = this.getAppSettings();
        final DiagnosticConfig newDiagnosticConfig = this.getDiagnosticConfig();
        final String newConfigurationSource = this.getConfigurationSource();
        // Using configuration from parent by default
        final String source = StringUtils.isBlank(newConfigurationSource) ? CONFIGURATION_SOURCE_PARENT : StringUtils.lowerCase(newConfigurationSource);

        final WebApp webApp = Objects.requireNonNull(this.getParent().getFullRemote());
        final DeploymentSlot.DefinitionStages.Blank blank = webApp.deploymentSlots().define(getName());
        final DeploymentSlot.DefinitionStages.WithCreate withCreate;
        if (CONFIGURATION_SOURCE_NEW.equals(source)) {
            withCreate = blank.withBrandNewConfiguration();
        } else if (CONFIGURATION_SOURCE_PARENT.equals(source)) {
            withCreate = blank.withConfigurationFromParent();
        } else {
            try {
                final DeploymentSlot sourceSlot = webApp.deploymentSlots().getByName(newConfigurationSource);
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
        messager.info(AzureString.format("Start creating Web App deployment slot ({0})...", name));
        // As we can not update runtime for deployment slot during creation, so call update resource here
        DeploymentSlot slot = (DeploymentSlot) Objects.requireNonNull(this.doModify(() -> withCreate.create(), Status.CREATING));
        final boolean isRuntimeModified = Objects.nonNull(this.getRuntime()) || Objects.nonNull(this.getDockerConfiguration());
        if (isRuntimeModified) {
            final DeploymentSlot slotToUpdate = slot;
            slot = (DeploymentSlot) Objects.requireNonNull(this.doModify(() -> updateResourceInAzure(slotToUpdate), Status.CREATING));
        }
        messager.success(AzureString.format("Web App deployment slot ({0}) is successfully created", name));
        return slot;
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public DeploymentSlot updateResourceInAzure(@Nonnull WebSiteBase base) {
        DeploymentSlot remote = (DeploymentSlot) base;
        final Map<String, String> oldAppSettings = origin.getAppSettings();
        final Map<String, String> settingsToAdd = this.ensureConfig().getAppSettings();
        settingsToAdd.entrySet().removeAll(oldAppSettings.entrySet());
        final Set<String> settingsToRemove = this.ensureConfig().getAppSettingsToRemove().stream()
                .filter(key -> oldAppSettings.containsValue(key)).collect(Collectors.toSet());
        final Runtime newRuntime = this.ensureConfig().getRuntime();
        final DockerConfiguration newDockerConfig = this.ensureConfig().getDockerConfiguration();
        final DiagnosticConfig newDiagnosticConfig = this.ensureConfig().getDiagnosticConfig();


        final Runtime oldRuntime = Objects.requireNonNull(origin.getRuntime());
        boolean isRuntimeModified =  !oldRuntime.isDocker() && Objects.nonNull(newRuntime) && !Objects.equals(newRuntime, oldRuntime);
        boolean isDockerConfigurationModified = oldRuntime.isDocker() && Objects.nonNull(newDockerConfig);
        boolean isAppSettingsModified = MapUtils.isNotEmpty(settingsToAdd) || CollectionUtils.isNotEmpty(settingsToRemove);
        boolean modified = Objects.nonNull(newDiagnosticConfig) || isAppSettingsModified ||
                isRuntimeModified || isDockerConfigurationModified;

        if (modified) {
            final DeploymentSlotBase.Update<DeploymentSlot> update = remote.update();
            Optional.ofNullable(settingsToAdd).ifPresent(update::withAppSettings);
            Optional.ofNullable(settingsToRemove).ifPresent(s -> s.forEach(update::withoutAppSetting));
            Optional.ofNullable(newRuntime).ifPresent(r -> updateRuntime(update, r));
            Optional.ofNullable(newDockerConfig)
                    .ifPresent(dockerConfiguration -> updateDockerConfiguration(update, dockerConfiguration));
            Optional.ofNullable(newDiagnosticConfig)
                    .ifPresent(diagnosticConfig -> AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, diagnosticConfig));
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Web App deployment slot({0})...", remote.name()));
            remote = update.apply();
            messager.success(AzureString.format("Web App deployment slot({0}) is successfully updated", remote.name()));
        }
        return remote;
    }

    private void updateRuntime(@Nonnull DeploymentSlotBase.Update<?> update, @Nonnull Runtime newRuntime) {
        final Runtime oldRuntime = Objects.requireNonNull(super.getRuntime());
        if (newRuntime.getOperatingSystem() != null && Objects.requireNonNull(oldRuntime).getOperatingSystem() != newRuntime.getOperatingSystem()) {
            throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
        }
        final OperatingSystem operatingSystem =
                ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), Objects.requireNonNull(oldRuntime).getOperatingSystem());
        if (operatingSystem == OperatingSystem.LINUX) {
            AzureMessager.getMessager().warning("Update runtime is not supported for Linux app service");
        } else if (operatingSystem == OperatingSystem.WINDOWS) {
            update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                    .withWebContainer(AppServiceUtils.toWebContainer(newRuntime));
        } else if (operatingSystem == OperatingSystem.DOCKER) {
            return; // skip for docker, as docker configuration will be handled in `updateDockerConfiguration`
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }
    }

    private void updateDockerConfiguration(@Nonnull DeploymentSlotBase.Update<?> update, @Nonnull DockerConfiguration newConfig) {
        final DeploymentSlotBase.UpdateStages.WithStartUpCommand<?> draft;
        if (StringUtils.isAllEmpty(newConfig.getUserName(), newConfig.getPassword())) {
            draft = update.withPublicDockerHubImage(newConfig.getImage());
        } else if (StringUtils.isEmpty(newConfig.getRegistryUrl())) {
            draft = update.withPrivateDockerHubImage(newConfig.getImage())
                    .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        } else {
            draft = update.withPrivateRegistryImage(newConfig.getImage(), newConfig.getRegistryUrl())
                    .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        }
        draft.withStartUpCommand(newConfig.getStartUpCommand());
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
        return Optional.ofNullable(config).map(Config::getDiagnosticConfig).orElseGet(super::getDiagnosticConfig);
    }

    @Nullable
    @Override
    public Runtime getRuntime() {
        return Optional.ofNullable(config).map(WebAppDeploymentSlotDraft.Config::getRuntime).orElseGet(super::getRuntime);
    }

    public void setRuntime(final Runtime runtime) {
        this.ensureConfig().setRuntime(runtime);
    }

    public void setDockerConfiguration(DockerConfiguration dockerConfiguration) {
        this.ensureConfig().setDockerConfiguration(dockerConfiguration);
    }

    @Nullable
    public DockerConfiguration getDockerConfiguration() {
        return Optional.ofNullable(config).map(WebAppDeploymentSlotDraft.Config::getDockerConfiguration).orElse(null);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) || (StringUtils.isBlank(this.config.getConfigurationSource()) &&
                CollectionUtils.isEmpty(this.config.getAppSettingsToRemove()) &&
                Objects.isNull(this.getDockerConfiguration()) &&
                Objects.equals(this.getDiagnosticConfig(), super.getDiagnosticConfig()) &&
                Objects.equals(this.getAppSettings(), super.getAppSettings()) &&
                Objects.equals(this.getRuntime(), super.getRuntime()));
        return !notModified;
    }

    @Data
    @Nullable
    private static class Config {
        private Runtime runtime;
        private DockerConfiguration dockerConfiguration;
        private String configurationSource;
        private DiagnosticConfig diagnosticConfig = null;
        private Set<String> appSettingsToRemove = new HashSet<>();
        private Map<String, String> appSettings = null;
    }
}
