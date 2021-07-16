/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages.Blank;
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages.WithCreate;
import com.azure.resourcemanager.appservice.models.FunctionApp.DefinitionStages.WithDockerContainerImage;
import com.azure.resourcemanager.appservice.models.FunctionApp.Update;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.AbstractAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionApp extends FunctionAppBase<com.azure.resourcemanager.appservice.models.FunctionApp, FunctionAppEntity> implements IFunctionApp {
    public static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8;
    private static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
    private final AzureResourceManager azureClient;

    public FunctionApp(@Nonnull final String id, @Nonnull final AzureResourceManager azureClient) {
        super(id);
        this.azureClient = azureClient;
    }

    public FunctionApp(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name,
                  @Nonnull final AzureResourceManager azureClient) {
        super(subscriptionId, resourceGroup, name);
        this.azureClient = azureClient;
    }

    public FunctionApp(@Nonnull WebSiteBase webSiteBase, @Nonnull final AzureResourceManager azureClient) {
        super(webSiteBase);
        this.azureClient = azureClient;
    }

    @Override
    public IAppServicePlan plan() {
        return Azure.az(AzureAppService.class).appServicePlan(getRemoteResource().appServicePlanId());
    }

    @Override
    public IAppServiceCreator<? extends IFunctionApp> create() {
        return new FunctionAppCreator();
    }

    @Override
    public IAppServiceUpdater<? extends IFunctionApp> update() {
        return new FunctionAppUpdater();
    }

    @Override
    @Cacheable(cacheName = "appservice/functionapp/{}/slot/{}", key = "${this.name()}/$slotName")
    public IFunctionAppDeploymentSlot deploymentSlot(String slotName) {
        return new FunctionAppDeploymentSlot(getRemoteResource(), slotName, azureClient);
    }

    @Override
    @Cacheable(cacheName = "appservice/functionapp/{}/slots", key = "${this.name()}", condition = "!(force&&force[0])")
    public List<IFunctionAppDeploymentSlot> deploymentSlots(boolean... force) {
        return getRemoteResource().deploymentSlots().list().stream().parallel()
                .map(functionSlotBasic -> new FunctionAppDeploymentSlot(functionSlotBasic, azureClient))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheName = "appservice/functionapp/{}/functions", key = "${this.name()}", condition = "!(force&&force[0])")
    public List<FunctionEntity> listFunctions(boolean... force) {
        return azureClient.functionApps()
                .listFunctions(getRemoteResource().resourceGroupName(), getRemoteResource().name()).stream()
                .map(AppServiceUtils::fromFunctionAppEnvelope)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void triggerFunction(String functionName, Object input) {
        getRemoteResource().triggerFunction(functionName, input);
    }

    @Override
    public void swap(String slotName) {
        getRemoteResource().swap(slotName);
    }

    @Override
    public void syncTriggers() {
        getRemoteResource().syncTriggers();
    }

    @Override
    public void delete() {
        azureClient.functionApps().deleteById(getRemoteResource().id());
    }

    @Nonnull
    @Override
    protected FunctionAppEntity getEntityFromRemoteResource(@NotNull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        return AppServiceUtils.fromFunctionApp(remote);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appservice.models.FunctionApp remote() {
        return azureClient.functionApps().getByResourceGroup(resourceGroup, name);
    }

    @Override
    public String getMasterKey() {
        return getRemoteResource().getMasterKey();
    }

    @Override
    public Map<String, String> listFunctionKeys(String functionName) {
        return getRemoteResource().listFunctionKeys(functionName);
    }

    public class FunctionAppCreator extends AbstractAppServiceCreator<FunctionApp> {
        public static final String APP_SETTING_MACHINEKEY_DECRYPTION_KEY = "MACHINEKEY_DecryptionKey";
        public static final String APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE = "WEBSITES_ENABLE_APP_SERVICE_STORAGE";
        public static final String APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE = "false";
        public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE = "FUNCTION_APP_EDIT_MODE";
        public static final String APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY = "readOnly";

        @Override
        public FunctionApp commit() {
            final Blank blank = FunctionApp.this.azureClient.functionApps().define(getName());
            final Runtime runtime = getRuntime();
            final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlan(getAppServicePlanEntity(), azureClient);
            if (appServicePlan == null) {
                throw new AzureToolkitRuntimeException("Target app service plan not exists");
            }
            final ResourceGroup resourceGroup = FunctionApp.this.azureClient.resourceGroups().getByName(getResourceGroup());
            final WithCreate withCreate;
            switch (runtime.getOperatingSystem()) {
                case LINUX:
                    withCreate = createLinuxFunctionApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case WINDOWS:
                    withCreate = createWindowsFunctionApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case DOCKER:
                    final DockerConfiguration dockerConfiguration = getDockerConfiguration().get();
                    withCreate = createDockerFunctionApp(blank, resourceGroup, appServicePlan, dockerConfiguration);
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
            FunctionApp.this.remote = withCreate.create();
            FunctionApp.this.entity = AppServiceUtils.fromFunctionApp(FunctionApp.this.remote);
            return FunctionApp.this;
        }

        WithCreate createWindowsFunctionApp(Blank blank, ResourceGroup resourceGroup, com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan,
                                            Runtime runtime) {
            return (WithCreate) blank.withExistingAppServicePlan(appServicePlan)
                    .withExistingResourceGroup(resourceGroup)
                    .withJavaVersion(AppServiceUtils.toWindowsJavaVersion(runtime))
                    .withWebContainer(null);
        }

        WithCreate createLinuxFunctionApp(Blank blank, ResourceGroup resourceGroup, com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan,
                                          Runtime runtime) {
            return blank.withExistingLinuxAppServicePlan(appServicePlan)
                    .withExistingResourceGroup(resourceGroup)
                    .withBuiltInImage(AppServiceUtils.toFunctionRuntimeStack(runtime));
        }

        WithCreate createDockerFunctionApp(Blank blank, ResourceGroup resourceGroup, com.azure.resourcemanager.appservice.models.AppServicePlan appServicePlan,
                                           DockerConfiguration dockerConfiguration) {
            // check service plan, consumption is not supported
            if (StringUtils.equalsIgnoreCase(appServicePlan.pricingTier().toSkuDescription().tier(), "Dynamic")) {
                throw new AzureToolkitRuntimeException("Docker function is not supported in consumption service plan");
            }
            final WithDockerContainerImage withLinuxAppFramework =
                    blank.withExistingLinuxAppServicePlan(appServicePlan).withExistingResourceGroup(resourceGroup);
            WithCreate withCreate;
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                withCreate = withLinuxAppFramework.withPublicDockerHubImage(dockerConfiguration.getImage());
            } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                withCreate = withLinuxAppFramework.withPrivateDockerHubImage(dockerConfiguration.getImage())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            } else {
                withCreate = withLinuxAppFramework.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
            final String decryptionKey = generateDecryptionKey();
            return (WithCreate) withCreate.withAppSetting(APP_SETTING_MACHINEKEY_DECRYPTION_KEY, decryptionKey)
                    .withAppSetting(APP_SETTING_WEBSITES_ENABLE_APP_SERVICE_STORAGE, APP_SETTING_DISABLE_WEBSITES_APP_SERVICE_STORAGE)
                    .withAppSetting(APP_SETTING_FUNCTION_APP_EDIT_MODE, APP_SETTING_FUNCTION_APP_EDIT_MODE_READONLY);
        }

        protected String generateDecryptionKey() {
            // Refers https://github.com/Azure/azure-cli/blob/dev/src/azure-cli/azure/cli/command_modules/appservice/custom.py#L2300
            return Hex.encodeHexString(RandomUtils.nextBytes(32)).toUpperCase();
        }
    }

    public class FunctionAppUpdater extends AbstractAppServiceUpdater<FunctionApp> {
        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";
        private boolean modified = false;

        @Override
        public FunctionApp commit() {
            Update update = getRemoteResource().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (getDockerConfiguration() != null && getDockerConfiguration().isPresent() && FunctionApp.this.getRuntime().isDocker()) {
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
                FunctionApp.this.remote = update.apply();
            }
            FunctionApp.this.entity = AppServiceUtils.fromFunctionApp(FunctionApp.this.remote);
            return FunctionApp.this;
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
            final Runtime current = FunctionApp.this.getRuntime();
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
                    return update.withBuiltInImage(AppServiceUtils.toFunctionRuntimeStack(newRuntime));
                case WINDOWS:
                    return updateWindowsFunctionApp(update, current, newRuntime);
                default:
                    throw new AzureToolkitRuntimeException(String.format(UNSUPPORTED_OPERATING_SYSTEM, newRuntime.getOperatingSystem()));
            }
        }

        private Update updateDockerConfiguration(Update update, DockerConfiguration dockerConfiguration) {
            if (StringUtils.isAllEmpty(dockerConfiguration.getUserName(), dockerConfiguration.getPassword())) {
                return update.withPublicDockerHubImage(dockerConfiguration.getImage());
            } else if (StringUtils.isEmpty(dockerConfiguration.getRegistryUrl())) {
                return update.withPrivateDockerHubImage(dockerConfiguration.getImage())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            } else {
                return update.withPrivateRegistryImage(dockerConfiguration.getImage(), dockerConfiguration.getRegistryUrl())
                        .withCredentials(dockerConfiguration.getUserName(), dockerConfiguration.getPassword());
            }
        }

        Update updateWindowsFunctionApp(Update update, Runtime currentRuntime, Runtime newRuntime) {
            if (currentRuntime.getJavaVersion() == JavaVersion.OFF) {
                final JavaVersion javaVersion = Optional.ofNullable(newRuntime.getJavaVersion()).orElse(DEFAULT_JAVA_VERSION);
                return (Update) update.withJavaVersion(AppServiceUtils.toJavaVersion(javaVersion)).withWebContainer(null);
            } else if (newRuntime.getJavaVersion() != JavaVersion.OFF && newRuntime.getJavaVersion() != currentRuntime.getJavaVersion()) {
                return (Update) update.withJavaVersion(AppServiceUtils.toJavaVersion(newRuntime.getJavaVersion())).withWebContainer(null);
            } else {
                return update;
            }
        }
    }
}
