/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentModule;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountModule;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainer;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerDraft;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

public class CreateOrUpdateFunctionAppTask extends AzureTask<FunctionAppBase<?, ?, ?>> {
    public static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHTS_CREATE_FAILED = "Unable to create the Application Insights " +
        "for the Function App due to error %s. Please use the Azure Portal to manually create and configure the " +
        "Application Insights if needed.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java.";
    private static final String CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING = "App setting `FUNCTIONS_WORKER_RUNTIME` doesn't " +
        "meet the requirement of Azure Java Functions, the value should be `java`.";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~4";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
        "isn't configured, setting up the default value.";
    private static final String FUNCTION_APP_NOT_EXIST_FOR_SLOT = "The Function App specified in pom.xml does not exist. " +
        "Please make sure the Function App name is correct.";

    public static final String FLEX_CONSUMPTION_SLOT_NOT_SUPPORT = "Deployment slot is not supported for function app with consumption plan.";

    private final FunctionAppConfig functionAppConfig;
    private final List<AzureTask<?>> tasks = new ArrayList<>();

    private ResourceGroup resourceGroup;
    private AppServicePlan appServicePlan;
    private StorageAccount storageAccount;
    private StorageAccount deploymentStorageAccount;
    private BlobContainer deploymentContainer;
    private ContainerAppsEnvironment environment;
    private String instrumentationKey;
    private ApplicationInsight applicationInsight;
    private FunctionAppBase<?, ?, ?> functionApp;


    public CreateOrUpdateFunctionAppTask(@Nonnull final FunctionAppConfig config) {
        this.functionAppConfig = config;
        initTasks();
    }

    private void initTasks() {
        final FunctionAppDraft appDraft = Azure.az(AzureFunctions.class).functionApps(functionAppConfig.subscriptionId())
            .updateOrCreate(functionAppConfig.appName(), functionAppConfig.resourceGroup());
        registerSubTask(getResourceGroupTask(), result -> this.resourceGroup = result);
        if (appDraft.isDraftForCreating()) {
            // create new storage account when create function app
            final String storageAccountName = StringUtils.firstNonBlank(functionAppConfig.storageAccountName(), getDefaultStorageAccountName(functionAppConfig.appName()));
            final String storageResourceGroup = StringUtils.firstNonBlank(functionAppConfig.storageAccountResourceGroup(), functionAppConfig.resourceGroup());
            registerSubTask(getStorageAccountTask(storageAccountName, storageResourceGroup), result -> this.storageAccount = result);
        }
        // get/create AI instances only if user didn't specify AI connection string in app settings
        final boolean isInstrumentKeyConfigured = MapUtils.isNotEmpty(functionAppConfig.appSettings()) &&
            functionAppConfig.appSettings().containsKey(APPINSIGHTS_INSTRUMENTATION_KEY);
        if (!functionAppConfig.disableAppInsights() && !isInstrumentKeyConfigured) {
            if (StringUtils.isNotEmpty(functionAppConfig.appInsightsKey())) {
                this.instrumentationKey = functionAppConfig.appInsightsKey();
            } else if (StringUtils.isNotEmpty(functionAppConfig.appInsightsInstance()) || !appDraft.exists()) {
                // create AI instance by default when create new function
                registerSubTask(getApplicationInsightsTask(), result -> {
                    this.applicationInsight = result;
                    this.instrumentationKey = Optional.ofNullable(result).map(ApplicationInsight::getInstrumentationKey).orElse(null);
                });
            }
        }
        if (StringUtils.isNotBlank(functionAppConfig.environment())) {
            registerSubTask(getContainerAppEnvironmentTask(), result -> this.environment = result);
        } else {
            registerSubTask(getServicePlanTask(), result -> this.appServicePlan = result);
        }
        if (isFlexConsumptionFunctionApp(appDraft)) {
            // create new storage account when create function app
            registerSubTask(getDeploymentStorageAccount(), result -> this.deploymentStorageAccount = result);
            registerSubTask(getDeploymentStorageContainer(), result -> this.deploymentContainer = result);
        }
        if (StringUtils.isEmpty(functionAppConfig.deploymentSlotName())) {
            final AzureTask<FunctionApp> functionTask = appDraft.exists() ? getUpdateFunctionAppTask(appDraft) : getCreateFunctionAppTask(appDraft);
            registerSubTask(functionTask, result -> this.functionApp = result);
        } else {
            final FunctionAppDeploymentSlotDraft slotDraft = getFunctionDeploymentSlot(appDraft);
            final AzureTask<FunctionAppDeploymentSlot> slotTask = slotDraft.exists() ?
                getUpdateFunctionSlotTask(slotDraft) : getCreateFunctionSlotTask(slotDraft);
            registerSubTask(slotTask, result -> this.functionApp = result);
        }
    }

    private AzureTask<BlobContainer> getDeploymentStorageContainer() {
        return new AzureTask<>(() -> {
            final FlexConsumptionConfiguration flexConfiguration = functionAppConfig.flexConsumptionConfiguration();
            final String prefix = "app-package";
            final String appName = StringUtils.substringBefore(functionAppConfig.appName().replaceAll("[^a-zA-Z0-9]", ""), 32);
            final String randomNum = String.valueOf(new Random().nextInt(1000000));
            final String containerName = Optional.ofNullable(flexConfiguration).map(FlexConsumptionConfiguration::getDeploymentContainer)
                .orElseGet(() -> prefix + "-" + appName + "-" + randomNum);
            final BlobContainer container = deploymentStorageAccount.getBlobContainerModule().getOrDraft(containerName, functionAppConfig.resourceGroup());
            if (container.isDraftForCreating()) {
                ((BlobContainerDraft) container).commit();
            }
            return container;
        });
    }

    private AzureTask<StorageAccount> getDeploymentStorageAccount() {
        final FlexConsumptionConfiguration flexConsumptionConfiguration = functionAppConfig.flexConsumptionConfiguration();
        final String accountName = Optional.ofNullable(flexConsumptionConfiguration)
            .map(FlexConsumptionConfiguration::getDeploymentAccount).orElse(null);
        final String resourceGroupName = Optional.ofNullable(flexConsumptionConfiguration)
            .map(FlexConsumptionConfiguration::getDeploymentResourceGroup).orElse(functionAppConfig.resourceGroup());
        if (StringUtils.isBlank(accountName)) {
            return new AzureTask<>(() -> storageAccount);
        }
        final ResourceGroup resourceGroup = Azure.az(AzureResources.class).groups(functionAppConfig.subscriptionId())
            .getOrDraft(resourceGroupName, resourceGroupName);
        if (!resourceGroup.exists()) {
            final Region region = getNonStageRegion(functionAppConfig.region());
            registerSubTask(new CreateResourceGroupTask(functionAppConfig.subscriptionId(), resourceGroupName, region), null);
        }
        return getStorageAccountTask(accountName, resourceGroupName);
    }

    private boolean isFlexConsumptionFunctionApp(final FunctionAppDraft appDraft) {
        if (appDraft.isDraftForCreating()) {
            return Optional.ofNullable(functionAppConfig.getPricingTier()).map(PricingTier::isFlexConsumption).orElse(false);
        }
        return Optional.ofNullable(appDraft.getAppServicePlan())
            .map(AppServicePlan::getPricingTier) // get pricing tier of the app service plan
            .map(PricingTier::isFlexConsumption)
            .orElse(false);
    }

    private AzureTask<ContainerAppsEnvironment> getContainerAppEnvironmentTask() {
        return new AzureTask<>(() -> {
            final String environmentName = functionAppConfig.environment();
            final String resourceGroupName = functionAppConfig.resourceGroup();
            final ContainerAppsEnvironmentModule environments = Azure.az(AzureContainerApps.class).environments(functionAppConfig.subscriptionId());
            final ContainerAppsEnvironment environment = environments.get(environmentName, resourceGroupName);
            if (environment != null && environment.exists()) {
                return environment;
            }
            final ContainerAppsEnvironmentDraft draft = environments.create(environmentName, resourceGroupName);
            final ContainerAppsEnvironmentDraft.Config config = new ContainerAppsEnvironmentDraft.Config();
            config.setName(environmentName);
            config.setRegion(getNonStageRegion(functionAppConfig.region()));
            config.setResourceGroup(this.resourceGroup);
            config.setLogAnalyticsWorkspace(Optional.ofNullable(this.applicationInsight).map(ApplicationInsight::getWorkspace).orElse(null));
            draft.setConfig(config);
            return draft.commit();
        });
    }

    private AzureTask<StorageAccount> getStorageAccountTask(final String storageAccountName, final String storageResourceGroup) {
        return new AzureTask<>(() -> {
            final StorageAccountModule accounts = Azure.az(AzureStorageAccount.class).accounts(functionAppConfig.subscriptionId());
            final StorageAccount existingAccount = accounts.get(storageAccountName, storageResourceGroup);
            if (existingAccount != null && existingAccount.exists()) {
                return existingAccount;
            }
            final StorageAccountDraft draft = accounts.create(storageAccountName, storageResourceGroup);
            draft.setRegion(getNonStageRegion(functionAppConfig.region()));
            draft.setKind(Kind.STORAGE_V2);
            draft.setRedundancy(Redundancy.STANDARD_LRS);
            return draft.commit();
        });
    }

    private String getDefaultStorageAccountName(@Nonnull final String functionAppName) {
        final ResourceManagerUtils.InternalRuntimeContext context = new ResourceManagerUtils.InternalRuntimeContext();
        return context.randomResourceName(functionAppName.replaceAll("[^a-zA-Z0-9]", ""), 20);
    }

    private static Region getNonStageRegion(@Nonnull final Region region) {
        final String regionName = region.getName();
        if (!StringUtils.containsIgnoreCase(regionName, "stage")) {
            return region;
        }
        return Optional.of(regionName)
            .map(name -> StringUtils.removeIgnoreCase(name, "(stage)"))
            .map(name -> StringUtils.removeIgnoreCase(name, "stage"))
            .map(StringUtils::trim)
            .map(Region::fromName).orElse(region);
    }

    private <T> void registerSubTask(AzureTask<T> task, Consumer<T> consumer) {
        if (task != null) {
            tasks.add(new AzureTask<>(() -> {
                T result = task.getBody().call();
                consumer.accept(result);
                return result;
            }));
        }
    }

    private AzureTask<FunctionApp> getCreateFunctionAppTask(final FunctionAppDraft draft) {
        final AzureString title = AzureString.format("Create new app({0}) on subscription({1})",
            functionAppConfig.appName(), functionAppConfig.subscriptionId());
        return new AzureTask<>(title, () -> {
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            Optional.ofNullable(instrumentationKey).filter(StringUtils::isNoneEmpty).ifPresent(key ->
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, key));
            draft.setAppServicePlan(appServicePlan);
            draft.setRegion(functionAppConfig.region());
            draft.setRuntime(getRuntime(functionAppConfig.runtime()));
            draft.setDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()));
            draft.setAppSettings(appSettings);
            draft.setDiagnosticConfig(functionAppConfig.diagnosticConfig());
            draft.setEnableDistributedTracing(functionAppConfig.enableDistributedTracing());
            draft.setFlexConsumptionConfiguration(functionAppConfig.flexConsumptionConfiguration());
            draft.setDeploymentAccount(deploymentStorageAccount);
            draft.setDeploymentContainer(deploymentContainer);
            draft.setStorageAccount(storageAccount);
            draft.setEnvironment(environment);
            draft.setContainerConfiguration(functionAppConfig.containerConfiguration());
            final FunctionApp result = draft.createIfNotExist();
            Thread.sleep(10 * 1000); // workaround for service initialization
            return result;
        });
    }

    private Map<String, String> processAppSettingsWithDefaultValue() {
        final Map<String, String> appSettings = Optional.ofNullable(functionAppConfig.appSettings()).orElseGet(HashMap::new);
        setDefaultAppSetting(appSettings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
            FUNCTIONS_EXTENSION_VERSION_VALUE, null);
        final OperatingSystem os = Optional.ofNullable(functionAppConfig.runtime()).map(RuntimeConfig::getOs).orElse(null);
        if (os != OperatingSystem.DOCKER) {
            setDefaultAppSetting(appSettings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING);
        }
        return appSettings;
    }

    private void setDefaultAppSetting(@Nonnull final Map<String, String> result, @Nonnull final String settingName, @Nonnull final String emptyMessage,
                                      @Nonnull final String defaultValue, @Nullable final String warningMessage) {
        final String setting = result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            AzureMessager.getMessager().info(emptyMessage);
            result.put(settingName, defaultValue);
            return;
        }
        // Show warning message when user set a different value
        if (!StringUtils.equalsIgnoreCase(setting, defaultValue) && StringUtils.isNotEmpty(warningMessage)) {
            AzureMessager.getMessager().warning(warningMessage);
        }
    }

    private AzureTask<FunctionApp> getUpdateFunctionAppTask(final FunctionAppDraft draft) {
        final AzureString title = AzureString.format("Update function app({0})", functionAppConfig.appName());
        return new AzureTask<>(title, () -> {
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            if (functionAppConfig.disableAppInsights()) {
                draft.removeAppSetting(APPINSIGHTS_INSTRUMENTATION_KEY);
            } else if (StringUtils.isNotEmpty(instrumentationKey)) {
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
            }
            draft.setAppServicePlan(appServicePlan);
            draft.setRuntime(getRuntime(functionAppConfig.runtime()));
            draft.setDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()));
            draft.setAppSettings(appSettings);
            draft.setDiagnosticConfig(functionAppConfig.diagnosticConfig());
            draft.removeAppSettings(functionAppConfig.appSettingsToRemove());
            draft.setFlexConsumptionConfiguration(functionAppConfig.flexConsumptionConfiguration());
            draft.setDeploymentAccount(this.deploymentStorageAccount);
            draft.setDeploymentContainer(this.deploymentContainer);
            draft.setEnableDistributedTracing(functionAppConfig.enableDistributedTracing());
            draft.setStorageAccount(storageAccount);
            return draft.updateIfExist();
        });
    }

    private AzureTask<FunctionAppDeploymentSlot> getCreateFunctionSlotTask(FunctionAppDeploymentSlotDraft draft) {
        final AzureString title = AzureString.format("Create new slot({0}) on function app ({1})",
            functionAppConfig.deploymentSlotName(), functionAppConfig.appName());
        return new AzureTask<>(title, () -> {
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            Optional.ofNullable(instrumentationKey).filter(StringUtils::isNoneEmpty).ifPresent(key ->
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, key));
            draft.setAppSettings(appSettings);
            draft.setRuntime(getRuntime(functionAppConfig.runtime()));
            draft.setDiagnosticConfig(functionAppConfig.diagnosticConfig());
            // draft.setFlexConsumptionConfiguration(functionAppConfig.flexConsumptionConfiguration());
            draft.setDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()));
            draft.setConfigurationSource(functionAppConfig.deploymentSlotConfigurationSource());
            draft.removeAppSettings(functionAppConfig.appSettingsToRemove());
            return draft.commit();
        });
    }

    private AzureTask<FunctionAppDeploymentSlot> getUpdateFunctionSlotTask(FunctionAppDeploymentSlotDraft draft) {
        final AzureString title = AzureString.format("Update function deployment slot({0})", functionAppConfig.deploymentSlotName());
        return new AzureTask<>(title, () -> {
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            if (functionAppConfig.disableAppInsights()) {
                draft.removeAppSetting(APPINSIGHTS_INSTRUMENTATION_KEY);
            } else if (StringUtils.isNotEmpty(instrumentationKey)) {
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
            }
            draft.setRuntime(getRuntime(functionAppConfig.runtime()));
            draft.setDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()));
            draft.setDiagnosticConfig(functionAppConfig.diagnosticConfig());
            // draft.setFlexConsumptionConfiguration(functionAppConfig.flexConsumptionConfiguration());
            draft.setAppSettings(appSettings);
            draft.removeAppSettings(functionAppConfig.appSettingsToRemove());
            return draft.commit();
        });
    }

    private FunctionAppDeploymentSlotDraft getFunctionDeploymentSlot(final FunctionApp functionApp) {
        if (!functionApp.exists()) {
            throw new AzureToolkitRuntimeException(FUNCTION_APP_NOT_EXIST_FOR_SLOT);
        }
        if (Objects.requireNonNull(functionApp.getAppServicePlan()).getPricingTier().isFlexConsumption()) {
            throw new AzureToolkitRuntimeException(FLEX_CONSUMPTION_SLOT_NOT_SUPPORT);
        }
        return functionApp.slots().updateOrCreate(functionAppConfig.deploymentSlotName(), functionAppConfig.resourceGroup());
    }

    private AzureTask<ApplicationInsight> getApplicationInsightsTask() {
        return new AzureTask<>(() -> {
            try {
                final String name = StringUtils.firstNonEmpty(functionAppConfig.appInsightsInstance(), functionAppConfig.appName());
                return new GetOrCreateApplicationInsightsTask(functionAppConfig.subscriptionId(),
                    functionAppConfig.resourceGroup(), getNonStageRegion(functionAppConfig.region()), name, functionAppConfig.workspaceConfig()).getBody().call();
            } catch (final Throwable e) {
                final String errorMessage = Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElse(e).getMessage();
                AzureMessager.getMessager().warning(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, errorMessage));
                return null;
            }
        });
    }

    private CreateResourceGroupTask getResourceGroupTask() {
        final Region region = functionAppConfig.region();
        final Region resourceGroupRegion = StringUtils.endsWith(region.getAbbreviation(), "(stage)") ?
            Region.fromName(StringUtils.removeIgnoreCase(region.getName(), "(stage)")) : region;
        return new CreateResourceGroupTask(functionAppConfig.subscriptionId(), functionAppConfig.resourceGroup(), resourceGroupRegion);
    }

    private AzureTask<AppServicePlan> getServicePlanTask() {
        if (StringUtils.isNotEmpty(functionAppConfig.deploymentSlotName())) {
            AzureMessager.getMessager().info("Skip update app service plan for deployment slot");
            return null;
        }
        if (StringUtils.isBlank(functionAppConfig.getServicePlanName())) {
            // users could keep using old service plan when update function app, return null in this case
            return null;
        }
        return new AzureTask<>(() -> {
            final AzureAppService az = Azure.az(AzureAppService.class);
            final AppServicePlanConfig config = FunctionAppConfig.getServicePlanConfig(functionAppConfig);
            final AppServicePlanDraft draft = az.plans(config.getSubscriptionId())
                .updateOrCreate(config.getName(), config.getResourceGroupName());
            draft.setOperatingSystem(config.getOs());
            draft.setRegion(config.getRegion());
            draft.setPricingTier(config.getPricingTier());
            return draft.commit();
        });
    }

    private FunctionAppRuntime getRuntime(RuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(runtimeConfig.os()).orElse(OperatingSystem.LINUX);
        FunctionAppRuntime runtime = null;
        if (os == OperatingSystem.DOCKER) {
            runtime = FunctionAppDockerRuntime.INSTANCE;
        } else if (os == OperatingSystem.LINUX) {
            runtime = FunctionAppLinuxRuntime.fromJavaVersionUserText(runtimeConfig.javaVersion());
        } else if (os == OperatingSystem.WINDOWS) {
            runtime = FunctionAppWindowsRuntime.fromJavaVersionUserText(runtimeConfig.javaVersion());
        }
        if (Objects.isNull(runtime) && (Objects.nonNull(runtimeConfig.os()) || StringUtils.isNotBlank(runtimeConfig.javaVersion()))) {
            throw new AzureToolkitRuntimeException("invalid runtime configuration, please refer to https://aka.ms/maven_function_configuration#supported-runtime for valid values");
        }
        return runtime;
    }

    // todo: remove duplicated with Create Web App Task
    private DockerConfiguration getDockerConfiguration(RuntimeConfig runtime) {
        if (OperatingSystem.DOCKER == runtime.os()) {
            return DockerConfiguration.builder()
                .userName(runtime.username())
                .password(runtime.password())
                .registryUrl(runtime.registryUrl())
                .image(runtime.image())
                .startUpCommand(runtime.startUpCommand())
                .build();
        }
        return null;
    }

    @Override
    public FunctionAppBase<?, ?, ?> doExecute() throws Exception {
        for (AzureTask<?> task : this.tasks) {
            task.getBody().call();
        }
        return functionApp;
    }
}
