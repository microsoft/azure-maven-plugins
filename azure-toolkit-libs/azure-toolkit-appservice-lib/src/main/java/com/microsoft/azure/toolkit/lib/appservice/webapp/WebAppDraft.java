/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.appservice.models.WebApp.UpdateStages;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WebAppDraft extends WebApp implements AzResource.Draft<WebApp, com.azure.resourcemanager.appservice.models.WebApp> {
    public static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
    public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";
    public static final Runtime DEFAULT_RUNTIME = Runtime.LINUX_JAVA17;

    @Getter
    @Nullable
    private final WebApp origin;
    @Nullable
    private Config config;

    WebAppDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull WebAppModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    WebAppDraft(@Nonnull WebApp origin) {
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
    @AzureOperation(name = "azure/webapp.create_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.WebApp createResourceInAzure() {
        final String name = getName();
        final Runtime newRuntime = Objects.requireNonNull(getRuntime(), "'runtime' is required to create Azure Web App");
        final AppServicePlan newPlan = Objects.requireNonNull(getAppServicePlan(), "'service plan' is required to create Azure Web App");
        final OperatingSystem os = newRuntime.isDocker() ? OperatingSystem.LINUX : newRuntime.getOperatingSystem();
        if (!Objects.equals(os, newPlan.getOperatingSystem())) {
            throw new AzureToolkitRuntimeException(String.format("Could not create %s app service in %s service plan", newRuntime.getOperatingSystem(), newPlan.getOperatingSystem()));
        }
        final Map<String, String> newAppSettings = getAppSettings();
        final DiagnosticConfig newDiagnosticConfig = getDiagnosticConfig();

        final AppServiceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final DefinitionStages.Blank blank = manager.webApps().define(name);
        final DefinitionStages.WithCreate withCreate;
        if (newRuntime.getOperatingSystem() == OperatingSystem.LINUX) {
            withCreate = blank.withExistingLinuxPlan(newPlan.getRemote())
                .withExistingResourceGroup(getResourceGroupName())
                .withBuiltInImage(AppServiceUtils.toRuntimeStack(newRuntime));
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.WINDOWS) {
            withCreate = (DefinitionStages.WithCreate) blank
                .withExistingWindowsPlan(newPlan.getRemote())
                .withExistingResourceGroup(getResourceGroupName())
                .withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                .withWebContainer(AppServiceUtils.toWebContainer(newRuntime));
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.DOCKER) {
            withCreate = createDockerWebApp(blank, newPlan);
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }

        if (MapUtils.isNotEmpty(newAppSettings)) { // todo: support remove app settings
            withCreate.withAppSettings(newAppSettings);
        }
        if (Objects.nonNull(newDiagnosticConfig)) {
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig);
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Web App({0})...", name));
        com.azure.resourcemanager.appservice.models.WebApp webApp = Objects.requireNonNull(withCreate.create());
        messager.success(AzureString.format("Web App({0}) is successfully created", name));
        return webApp;
    }

    DefinitionStages.WithCreate createDockerWebApp(@Nonnull DefinitionStages.Blank blank, @Nonnull AppServicePlan plan) {
        final String message = "Docker configuration is required to create a docker based Azure Web App";
        final DockerConfiguration config = Objects.requireNonNull(this.getDockerConfiguration(), message);
        final DefinitionStages.WithLinuxAppFramework withLinuxAppFramework = blank
            .withExistingLinuxPlan(plan.getRemote())
            .withExistingResourceGroup(getResourceGroupName());
        final DefinitionStages.WithStartUpCommand draft;
        if (StringUtils.isAllEmpty(config.getUserName(), config.getPassword())) {
            draft = withLinuxAppFramework.withPublicDockerHubImage(config.getImage());
        } else if (StringUtils.isEmpty(config.getRegistryUrl())) {
            draft = withLinuxAppFramework.withPrivateDockerHubImage(config.getImage()).withCredentials(config.getUserName(), config.getPassword());
        } else {
            draft = withLinuxAppFramework.withPrivateRegistryImage(config.getImage(), config.getRegistryUrl()).withCredentials(config.getUserName(), config.getPassword());
        }
        return draft.withStartUpCommand(config.getStartUpCommand());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/webapp.update_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.WebApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appservice.models.WebApp remote) {
        assert origin != null : "updating target is not specified.";
        final Map<String, String> oldAppSettings = Objects.requireNonNull(origin.getAppSettings());
        final Map<String, String> settingsToAdd = this.ensureConfig().getAppSettings();
        if (ObjectUtils.allNotNull(oldAppSettings, settingsToAdd)) {
            settingsToAdd.entrySet().removeAll(oldAppSettings.entrySet());
        }
        final Set<String> settingsToRemove = Optional.ofNullable(this.ensureConfig().getAppSettingsToRemove())
                .map(set -> set.stream().filter(oldAppSettings::containsKey).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        final DiagnosticConfig newDiagnosticConfig = this.ensureConfig().getDiagnosticConfig();
        final Runtime newRuntime = this.ensureConfig().getRuntime();
        final AppServicePlan newPlan = this.ensureConfig().getPlan();
        final DockerConfiguration newDockerConfig = this.ensureConfig().getDockerConfiguration();
        final Runtime oldRuntime = origin.getRuntime();
        final AppServicePlan oldPlan = origin.getAppServicePlan();

        final boolean planModified = Objects.nonNull(newPlan) && !Objects.equals(newPlan, oldPlan);
        final boolean runtimeModified = !Objects.requireNonNull(oldRuntime).isDocker() && Objects.nonNull(newRuntime) && !Objects.equals(newRuntime, oldRuntime);
        final boolean dockerModified = oldRuntime.isDocker() && Objects.nonNull(newDockerConfig);
        boolean modified = planModified || runtimeModified || dockerModified ||
            MapUtils.isNotEmpty(settingsToAdd) || CollectionUtils.isNotEmpty(settingsToRemove) || Objects.nonNull(newDiagnosticConfig);

        if (modified) {
            final Update update = remote.update();
            Optional.ofNullable(newPlan).ifPresent(p -> updateAppServicePlan(update, p));
            Optional.ofNullable(newRuntime).ifPresent(p -> updateRuntime(update, p));
            Optional.ofNullable(settingsToAdd).ifPresent(update::withAppSettings);
            Optional.of(settingsToRemove).filter(CollectionUtils::isNotEmpty).ifPresent(s -> s.forEach(update::withoutAppSetting));
            Optional.ofNullable(newDockerConfig).ifPresent(p -> updateDockerConfiguration(update, p));
            Optional.ofNullable(newDiagnosticConfig).ifPresent(c -> AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, newDiagnosticConfig));

            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Web App({0})...", remote.name()));
            remote = update.apply();
            messager.success(AzureString.format("Web App({0}) is successfully updated", remote.name()));
        }
        return remote;
    }

    private void updateAppServicePlan(@Nonnull Update update, @Nonnull AppServicePlan newPlan) {
        Objects.requireNonNull(newPlan.getRemote(), "Target app service plan doesn't exist");
        final OperatingSystem os = Objects.requireNonNull(getRuntime()).isDocker() ? OperatingSystem.LINUX : getRuntime().getOperatingSystem();
        if (!Objects.equals(os, newPlan.getOperatingSystem())) {
            throw new AzureToolkitRuntimeException(String.format("Could not migrate %s app service to %s service plan", getRuntime().getOperatingSystem(), newPlan.getOperatingSystem()));
        }
        update.withExistingAppServicePlan(newPlan.getRemote());
    }

    private void updateRuntime(@Nonnull Update update, @Nonnull Runtime newRuntime) {
        final Runtime oldRuntime = Objects.requireNonNull(Objects.requireNonNull(origin).getRuntime());
        if (newRuntime.getOperatingSystem() != null && (oldRuntime.isWindows() != newRuntime.isWindows())) {
            throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
        }
        final OperatingSystem operatingSystem = ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), oldRuntime.getOperatingSystem());
        if (operatingSystem == OperatingSystem.LINUX) {
            update.withBuiltInImage(AppServiceUtils.toRuntimeStack(newRuntime));
        } else if (operatingSystem == OperatingSystem.WINDOWS) {
            update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                .withWebContainer(AppServiceUtils.toWebContainer(newRuntime));
        } else if (operatingSystem == OperatingSystem.DOCKER) {
            return; // skip for docker, as docker configuration will be handled in `updateDockerConfiguration`
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }
    }

    private void updateDockerConfiguration(@Nonnull Update update, @Nonnull DockerConfiguration newConfig) {
        final UpdateStages.WithStartUpCommand draft;
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

    public void setRuntime(Runtime runtime) {
        this.ensureConfig().setRuntime(runtime);
    }

    @Nullable
    @Override
    public Runtime getRuntime() {
        return Optional.ofNullable(config).map(Config::getRuntime).orElseGet(super::getRuntime);
    }

    public void setAppServicePlan(AppServicePlan plan) {
        this.ensureConfig().setPlan(plan);
    }

    @Nullable
    @Override
    public AppServicePlan getAppServicePlan() {
        return Optional.ofNullable(config).map(Config::getPlan).orElseGet(super::getAppServicePlan);
    }

    public void setDiagnosticConfig(DiagnosticConfig config) {
        this.ensureConfig().setDiagnosticConfig(config);
    }

    @Nullable
    public DiagnosticConfig getDiagnosticConfig() {
        return Optional.ofNullable(config).map(Config::getDiagnosticConfig).orElseGet(super::getDiagnosticConfig);
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

    public void removeAppSettings(Set<String> keys) {
        this.ensureConfig().getAppSettingsToRemove().addAll(ObjectUtils.firstNonNull(keys, Collections.emptySet()));
    }

    @Nullable
    public Set<String> getAppSettingsToRemove() {
        return Optional.ofNullable(config).map(Config::getAppSettingsToRemove).orElse(new HashSet<>());
    }

    public void setDockerConfiguration(DockerConfiguration dockerConfiguration) {
        this.ensureConfig().setDockerConfiguration(dockerConfiguration);
    }

    @Nullable
    public DockerConfiguration getDockerConfiguration() {
        return Optional.ofNullable(config).map(Config::getDockerConfiguration).orElse(null);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getRuntime()) || Objects.equals(this.config.getRuntime(), super.getRuntime()) ||
            Objects.isNull(this.config.getPlan()) || Objects.equals(this.config.getPlan(), super.getAppServicePlan()) ||
            Objects.isNull(this.config.getDiagnosticConfig()) ||
            CollectionUtils.isEmpty(this.config.getAppSettingsToRemove()) ||
            Objects.isNull(this.config.getAppSettings()) || Objects.equals(this.config.getAppSettings(), super.getAppSettings()) ||
            Objects.isNull(this.config.getDockerConfiguration());
        return !notModified;
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    @Nullable
    private static class Config {
        private Runtime runtime;
        private AppServicePlan plan = null;
        private DiagnosticConfig diagnosticConfig = null;
        private Set<String> appSettingsToRemove = new HashSet<>();
        private Map<String, String> appSettings = null;
        private DockerConfiguration dockerConfiguration = null;
    }
}