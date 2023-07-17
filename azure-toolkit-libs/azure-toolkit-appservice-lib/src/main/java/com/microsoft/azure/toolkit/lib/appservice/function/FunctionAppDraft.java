/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.core.util.Context;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.WebAppsClient;
import com.azure.resourcemanager.appservice.fluent.models.SiteConfigResourceInner;
import com.azure.resourcemanager.appservice.fluent.models.SitePatchResourceInner;
import com.azure.resourcemanager.appservice.implementation.FunctionAppsImpl;
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.FunctionApp.Update;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
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
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionAppDraft extends FunctionApp implements AzResource.Draft<FunctionApp, com.azure.resourcemanager.appservice.models.FunctionApp> {
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    public static final String FUNCTIONS_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION";
    public static final JavaVersion DEFAULT_JAVA_VERSION = Runtime.DEFAULT_FUNCTION_RUNTIME.getJavaVersion();
    public static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
    public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system of an existing app";

    public static final String APP_SETTING_MACHINEKEY_DECRYPTION_KEY = "MACHINEKEY_DecryptionKey";
    public static final String APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE = "WEBSITES_ENABLE_APP_SERVICE_STORAGE";
    public static final String APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE = "false";
    public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE = "FUNCTION_APP_EDIT_MODE";
    public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY = "readOnly";

    @Getter
    @Nullable
    private final FunctionApp origin;
    @Nullable
    private Config config;

    FunctionAppDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull FunctionAppModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    FunctionAppDraft(@Nonnull FunctionApp origin) {
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
    @AzureOperation(name = "azure/function.create_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.FunctionApp createResourceInAzure() {
        OperationContext.action().setTelemetryProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));

        final String name = getName();
        final Runtime newRuntime = Objects.requireNonNull(getRuntime(), "'runtime' is required to create a Function App");
        final AppServicePlan newPlan = Objects.requireNonNull(getAppServicePlan(), "'service plan' is required to create a Function App");
        final OperatingSystem os = newRuntime.isDocker() ? OperatingSystem.LINUX : newRuntime.getOperatingSystem();
        if (!Objects.equals(os, newPlan.getOperatingSystem())) {
            throw new AzureToolkitRuntimeException(String.format("Could not create %s app service in %s service plan", newRuntime.getOperatingSystem(), newPlan.getOperatingSystem()));
        }
        final Map<String, String> newAppSettings = getAppSettings();
        final DiagnosticConfig newDiagnosticConfig = getDiagnosticConfig();
        final FlexConsumptionConfiguration newFlexConsumptionConfiguration = getFlexConsumptionConfiguration();
        final String funcExtVersion = Optional.ofNullable(newAppSettings).map(map -> map.get(FUNCTIONS_EXTENSION_VERSION)).orElse(null);
        final StorageAccount storageAccount = getStorageAccount();

        final AppServiceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final DefinitionStages.Blank blank = manager.functionApps().define(name);
        final DefinitionStages.WithCreate withCreate;
        if (newRuntime.getOperatingSystem() == OperatingSystem.LINUX) {
            withCreate = blank.withExistingLinuxAppServicePlan(newPlan.getRemote())
                .withExistingResourceGroup(getResourceGroupName())
                .withBuiltInImage(AppServiceUtils.toFunctionRuntimeStack(newRuntime, funcExtVersion));
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.WINDOWS) {
            withCreate = (DefinitionStages.WithCreate) blank
                .withExistingAppServicePlan(newPlan.getRemote())
                .withExistingResourceGroup(getResourceGroupName())
                .withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion()))
                .withWebContainer(null);
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.DOCKER) {
            withCreate = createDockerApp(blank, newPlan);
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }
        if (MapUtils.isNotEmpty(newAppSettings)) {
            // todo: support remove app settings
            withCreate.withAppSettings(newAppSettings);
        }
        if (Objects.nonNull(storageAccount)) {
            withCreate.withExistingStorageAccount(storageAccount.getRemote());
        }
        if (Objects.nonNull(newDiagnosticConfig)) {
            AppServiceUtils.defineDiagnosticConfigurationForWebAppBase(withCreate, newDiagnosticConfig);
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        final boolean updateFlexConsumptionConfiguration = Objects.nonNull(newFlexConsumptionConfiguration) && getAppServicePlan().getPricingTier().isFlexConsumption();
        if (updateFlexConsumptionConfiguration) {
            ((com.azure.resourcemanager.appservice.models.FunctionApp) withCreate).innerModel().withContainerSize(newFlexConsumptionConfiguration.getInstanceSize());
        }
        messager.info(AzureString.format("Start creating Function App({0})...", name));
        com.azure.resourcemanager.appservice.models.FunctionApp functionApp = (com.azure.resourcemanager.appservice.models.FunctionApp)
            Objects.requireNonNull(this.doModify(() -> {
                com.azure.resourcemanager.appservice.models.FunctionApp app = withCreate.create();
                if (updateFlexConsumptionConfiguration) {
                    updateFlexConsumptionConfiguration(app, newFlexConsumptionConfiguration);
                }
                return app;
            }, Status.CREATING));
        messager.success(AzureString.format("Function App({0}) is successfully created", name));
        return functionApp;
    }

    DefinitionStages.WithCreate createDockerApp(@Nonnull DefinitionStages.Blank blank, @Nonnull AppServicePlan plan) {
        // check service plan, consumption is not supported
        final String message = "Docker configuration is required to create a docker based Function app";
        final DockerConfiguration config = Objects.requireNonNull(this.getDockerConfiguration(), message);
        if (StringUtils.equalsIgnoreCase(plan.getPricingTier().getTier(), "Dynamic")) {
            throw new AzureToolkitRuntimeException("Docker function is not supported in consumption service plan");
        }
        final DefinitionStages.WithDockerContainerImage withLinuxAppFramework = blank
            .withExistingLinuxAppServicePlan(plan.getRemote())
            .withExistingResourceGroup(getResourceGroupName());
        final DefinitionStages.WithCreate draft;
        if (StringUtils.isAllEmpty(config.getUserName(), config.getPassword())) {
            draft = withLinuxAppFramework
                .withPublicDockerHubImage(config.getImage());
        } else if (StringUtils.isEmpty(config.getRegistryUrl())) {
            draft = withLinuxAppFramework
                .withPrivateDockerHubImage(config.getImage())
                .withCredentials(config.getUserName(), config.getPassword());
        } else {
            draft = withLinuxAppFramework
                .withPrivateRegistryImage(config.getImage(), config.getRegistryUrl())
                .withCredentials(config.getUserName(), config.getPassword());
        }
        final String decryptionKey = generateDecryptionKey();
        return (DefinitionStages.WithCreate) draft.withAppSetting(APP_SETTING_MACHINEKEY_DECRYPTION_KEY, decryptionKey)
            .withAppSetting(APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE)
            .withAppSetting(APP_SETTING_FUNCTION_APP_EDIT_MODE, APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY);
    }

    protected String generateDecryptionKey() {
        // Refers https://github.com/Azure/azure-cli/blob/dev/src/azure-cli/azure/cli/command_modules/appservice/custom.py#L2300
        return Hex.encodeHexString(RandomUtils.nextBytes(32)).toUpperCase();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/function.update_app.app", params = {"this.getName()"})
    public com.azure.resourcemanager.appservice.models.FunctionApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        assert origin != null : "updating target is not specified.";
        final Map<String, String> oldAppSettings = Objects.requireNonNull(origin.getAppSettings());
        final Map<String, String> settingsToAdd = this.ensureConfig().getAppSettings();
        if (ObjectUtils.allNotNull(oldAppSettings, settingsToAdd)) {
            settingsToAdd.entrySet().removeAll(oldAppSettings.entrySet());
        }
        final Set<String> settingsToRemove = Optional.ofNullable(this.ensureConfig().getAppSettingsToRemove())
                .map(set -> set.stream().filter(oldAppSettings::containsKey).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        final DiagnosticConfig oldDiagnosticConfig = super.getDiagnosticConfig();
        final DiagnosticConfig newDiagnosticConfig = this.ensureConfig().getDiagnosticConfig();
        final Runtime newRuntime = this.ensureConfig().getRuntime();
        final AppServicePlan newPlan = this.ensureConfig().getPlan();
        final DockerConfiguration newDockerConfig = this.ensureConfig().getDockerConfiguration();
        final FlexConsumptionConfiguration newFlexConsumptionConfiguration = this.ensureConfig().getFlexConsumptionConfiguration();
        final StorageAccount storageAccount = getStorageAccount();
        final Runtime oldRuntime = Objects.requireNonNull(origin.getRuntime());
        final AppServicePlan oldPlan = origin.getAppServicePlan();
        final FlexConsumptionConfiguration oldFlexConsumptionConfiguration = origin.getFlexConsumptionConfiguration();

        final boolean planModified = Objects.nonNull(newPlan) && !Objects.equals(newPlan, oldPlan);
        final boolean runtimeModified = !oldRuntime.isDocker() && Objects.nonNull(newRuntime) && !Objects.equals(newRuntime, oldRuntime);
        final boolean dockerModified = oldRuntime.isDocker() && Objects.nonNull(newDockerConfig);
        final boolean flexConsumptionModified = getAppServicePlan().getPricingTier().isFlexConsumption() &&
            Objects.nonNull(newFlexConsumptionConfiguration) && !Objects.equals(newFlexConsumptionConfiguration, oldFlexConsumptionConfiguration);
        final boolean isAppSettingsModified = MapUtils.isNotEmpty(settingsToAdd) || CollectionUtils.isNotEmpty(settingsToRemove);
        final boolean isDiagnosticConfigModified = Objects.nonNull(newDiagnosticConfig) && !Objects.equals(newDiagnosticConfig, oldDiagnosticConfig);
        final boolean modified = planModified || runtimeModified || dockerModified || flexConsumptionModified ||
            isAppSettingsModified || Objects.nonNull(newDiagnosticConfig) || Objects.nonNull(storageAccount);
        final String funcExtVersion = Optional.ofNullable(settingsToAdd).map(map -> map.get(FUNCTIONS_EXTENSION_VERSION))
                .orElseGet(() -> oldAppSettings.get(FUNCTIONS_EXTENSION_VERSION));
        if (modified) {
            final Update update = remote.update();
            Optional.ofNullable(newPlan).filter(ignore -> planModified).ifPresent(p -> updateAppServicePlan(update, p));
            Optional.ofNullable(newRuntime).filter(ignore -> runtimeModified).ifPresent(p -> updateRuntime(update, p, funcExtVersion));
            Optional.ofNullable(settingsToAdd).filter(ignore -> isAppSettingsModified).ifPresent(update::withAppSettings);
            Optional.of(settingsToRemove).filter(CollectionUtils::isNotEmpty).filter(ignore -> isAppSettingsModified).ifPresent(s -> s.forEach(update::withoutAppSetting));
            Optional.ofNullable(newDockerConfig).filter(ignore -> dockerModified).ifPresent(p -> updateDockerConfiguration(update, p));
            Optional.ofNullable(newDiagnosticConfig).filter(ignore -> isDiagnosticConfigModified).ifPresent(c -> AppServiceUtils.updateDiagnosticConfigurationForWebAppBase(update, c));
            Optional.ofNullable(storageAccount).ifPresent(s -> update.withExistingStorageAccount(s.getRemote()));
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating Function App({0})...", remote.name()));
            remote = update.apply();
            if (flexConsumptionModified) {
                updateFlexConsumptionConfiguration(remote, newFlexConsumptionConfiguration);
            }
            messager.success(AzureString.format("Function App({0}) is successfully updated", remote.name()));
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

    private void updateRuntime(@Nonnull Update update, @Nonnull Runtime newRuntime, String funcExtVersion) {
        final Runtime oldRuntime = Objects.requireNonNull(Objects.requireNonNull(origin).getRuntime());
        if (newRuntime.getOperatingSystem() != null && oldRuntime.getOperatingSystem() != newRuntime.getOperatingSystem()) {
            throw new AzureToolkitRuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
        }
        final OperatingSystem operatingSystem = ObjectUtils.firstNonNull(newRuntime.getOperatingSystem(), oldRuntime.getOperatingSystem());
        if (operatingSystem == OperatingSystem.LINUX) {
            update.withBuiltInImage(AppServiceUtils.toFunctionRuntimeStack(newRuntime, funcExtVersion));
        } else if (operatingSystem == OperatingSystem.WINDOWS) {
            if (Objects.equals(oldRuntime.getJavaVersion(), JavaVersion.OFF)) {
                final JavaVersion javaVersion = Optional.ofNullable(newRuntime.getJavaVersion()).orElse(DEFAULT_JAVA_VERSION);
                update.withJavaVersion(AppServiceUtils.toJavaVersion(javaVersion)).withWebContainer(null);
            } else if (ObjectUtils.notEqual(newRuntime.getJavaVersion(), JavaVersion.OFF) &&
                ObjectUtils.notEqual(newRuntime.getJavaVersion(), oldRuntime.getJavaVersion())) {
                update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion())).withWebContainer(null);
            }
        } else if (newRuntime.getOperatingSystem() == OperatingSystem.DOCKER) {
            return; // skip for docker, as docker configuration will be handled in `updateDockerConfiguration`
        } else {
            throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
        }
    }

    private void updateDockerConfiguration(@Nonnull Update update, @Nonnull DockerConfiguration newConfig) {
        if (StringUtils.isAllEmpty(newConfig.getUserName(), newConfig.getPassword())) {
            update.withPublicDockerHubImage(newConfig.getImage());
        } else if (StringUtils.isEmpty(newConfig.getRegistryUrl())) {
            update.withPrivateDockerHubImage(newConfig.getImage())
                .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        } else {
            update.withPrivateRegistryImage(newConfig.getImage(), newConfig.getRegistryUrl())
                .withCredentials(newConfig.getUserName(), newConfig.getPassword());
        }
    }

    public void setRuntime(Runtime runtime) {
        this.ensureConfig().setRuntime(runtime);
    }

    @Nullable
    @Override
    public Runtime getRuntime() {
        return Optional.ofNullable(config).map(Config::getRuntime).orElseGet(super::getRuntime);
    }

    public void setStorageAccount(StorageAccount account) {
        this.ensureConfig().setStorageAccount(account);
    }

    public StorageAccount getStorageAccount() {
        return Optional.ofNullable(config).map(Config::getStorageAccount).orElse(null);
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

    public void setAppSetting(String key, String value) {
        this.ensureConfig().getAppSettings().put(key, value);
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

    public void setFlexConsumptionConfiguration(FlexConsumptionConfiguration flexConsumptionConfiguration) {
        this.ensureConfig().setFlexConsumptionConfiguration(flexConsumptionConfiguration);
    }

    @Nullable
    public FlexConsumptionConfiguration getFlexConsumptionConfiguration() {
        return Optional.ofNullable(config).map(Config::getFlexConsumptionConfiguration).orElseGet(super::getFlexConsumptionConfiguration);
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
        private StorageAccount storageAccount = null;
        private DiagnosticConfig diagnosticConfig = null;
        private Set<String> appSettingsToRemove = new HashSet<>();
        private Map<String, String> appSettings = new HashMap<>();
        private DockerConfiguration dockerConfiguration = null;
        private FlexConsumptionConfiguration flexConsumptionConfiguration;
    }

    private static void updateFlexConsumptionConfiguration(final com.azure.resourcemanager.appservice.models.FunctionApp app, final FlexConsumptionConfiguration flexConfiguration) {
        final WebAppsClient webApps = app.manager().serviceClient().getWebApps();
        if (ObjectUtils.anyNotNull(flexConfiguration.getMaximumInstances(), flexConfiguration.getAlwaysReadyInstances())) {
            final SiteConfigResourceInner configuration = webApps.getConfiguration(app.resourceGroupName(), app.name());
            Optional.ofNullable(flexConfiguration.getMaximumInstances())
                .filter(maxInstances -> Objects.equals(maxInstances, configuration.functionAppScaleLimit()))
                .ifPresent(configuration::withFunctionAppScaleLimit);
            Optional.ofNullable(flexConfiguration.getAlwaysReadyInstances())
                .filter(readyInstances -> Objects.equals(readyInstances, configuration.minimumElasticInstanceCount()))
                .ifPresent(configuration::withMinimumElasticInstanceCount);
            webApps.updateConfiguration(app.resourceGroupName(), app.name(), configuration);
        }
        if (Objects.equals(app.innerModel().containerSize(), flexConfiguration.getInstanceSize())) {
            webApps.updateWithResponse(app.resourceGroupName(), app.name(), new SitePatchResourceInner()
                .withContainerSize(flexConfiguration.getInstanceSize()), Context.NONE);
        }
    }
}