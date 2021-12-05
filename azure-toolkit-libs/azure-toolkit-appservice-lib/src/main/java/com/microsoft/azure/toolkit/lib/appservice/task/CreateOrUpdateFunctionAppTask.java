/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsEntity;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class CreateOrUpdateFunctionAppTask extends AzureTask<IFunctionAppBase<?>> {
    public static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHTS_CREATE_FAILED = "Unable to create the Application Insights " +
            "for the Function App due to error %s. Please use the Azure Portal to manually create and configure the " +
            "Application Insights if needed.";
    private static final String CREATE_FUNCTION_APP = "Creating function app %s...";
    private static final String CREATE_FUNCTION_APP_DONE = "Successfully created function app %s.";
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    private static final String UPDATE_FUNCTION_APP = "Updating target Function App %s...";
    private static final String UPDATE_FUNCTION_DONE = "Successfully updated Function App %s.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java.";
    private static final String CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING = "App setting `FUNCTIONS_WORKER_RUNTIME` doesn't " +
            "meet the requirement of Azure Java Functions, the value should be `java`.";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
            "isn't configured, setting up the default value.";
    private static final String FUNCTION_APP_NOT_EXIST_FOR_SLOT = "The Function App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    private static final String FUNCTION_SLOT_CREATE_START = "The specified function slot does not exist. " +
            "Creating a new slot...";
    private static final String FUNCTION_SLOT_CREATED = "Successfully created the function slot: %s.";
    private static final String FUNCTION_SLOT_UPDATE = "Updating the specified function slot...";
    private static final String FUNCTION_SLOT_UPDATE_DONE = "Successfully updated the function slot: %s.";

    public static final JavaVersion DEFAULT_FUNCTION_JAVA_VERSION = JavaVersion.JAVA_8;

    private final FunctionAppConfig functionAppConfig;
    private final List<AzureTask<?>> tasks = new ArrayList<>();

    private ResourceGroup resourceGroup;
    private IAppServicePlan appServicePlan;
    private ApplicationInsightsEntity applicationInsights;
    private IFunctionAppBase<?> functionApp;


    public CreateOrUpdateFunctionAppTask(@Nonnull final FunctionAppConfig config) {
        this.functionAppConfig = config;
        initTasks();
    }

    private void initTasks() {
        final FunctionApp app = Azure.az(AzureAppService.class).subscription(functionAppConfig.subscriptionId())
                .functionApp(functionAppConfig.resourceGroup(), functionAppConfig.appName());
        registerSubTask(getResourceGroupTask(), result -> this.resourceGroup = result);
        registerSubTask(getServicePlanTask(), result -> this.appServicePlan = result);
        // get/create ai instances only if user didn't specify ai connection string in app settings
        if (!functionAppConfig.disableAppInsights() && !functionAppConfig.appSettings().containsKey(APPINSIGHTS_INSTRUMENTATION_KEY)) {
            if (StringUtils.isNotEmpty(functionAppConfig.appInsightsKey())) {
                this.applicationInsights = ApplicationInsightsEntity.builder().instrumentationKey(functionAppConfig.appInsightsKey()).build();
            } else if (StringUtils.isNotEmpty(functionAppConfig.appInsightsInstance()) || !app.exists()) {
                // create ai instance by default when create new function
                registerSubTask(getApplicationInsightsTask(), result -> this.applicationInsights = result);
            }
        }
        if (StringUtils.isEmpty(functionAppConfig.deploymentSlotName())) {
            final AzureTask<FunctionApp> functionTask = app.exists() ?
                    getUpdateFunctionAppTask(app) : getCreateFunctionAppTask(app);
            registerSubTask(functionTask, result -> this.functionApp = result);
        } else {
            final FunctionAppDeploymentSlot deploymentSlot = getFunctionDeploymentSlot(app);
            final AzureTask<FunctionAppDeploymentSlot> slotTask = deploymentSlot.exists() ?
                    getUpdateFunctionSlotTask(deploymentSlot) : getCreateFunctionSlotTask(deploymentSlot);
            registerSubTask(slotTask, result -> this.functionApp = result);
        }
    }

    private <T> void registerSubTask(AzureTask<T> task, Consumer<T> consumer) {
        tasks.add(new AzureTask<>(() -> {
            T result = task.getSupplier().get();
            consumer.accept(result);
            return result;
        }));
    }

    private AzureTask<FunctionApp> getCreateFunctionAppTask(final FunctionApp functionApp) {
        final AzureString title = AzureString.format("Create new app({0}) on subscription({1})",
                functionAppConfig.appName(), functionAppConfig.subscriptionId());
        return new AzureTask<>(title, () -> {
            AzureTelemetry.getActionContext().setProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
            AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP, functionAppConfig.appName()));
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            Optional.ofNullable(applicationInsights).ifPresent(insights ->
                    appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, applicationInsights.getInstrumentationKey()));
            final FunctionApp result = functionApp.create().withName(functionAppConfig.appName())
                    .withResourceGroup(resourceGroup.getName())
                    .withPlan(appServicePlan.id())
                    .withRuntime(getRuntime(functionAppConfig.runtime()))
                    .withDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()))
                    .withAppSettings(appSettings)
                    .commit();
            AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP_DONE, result.name()));
            return result;
        });
    }

    private Map<String, String> processAppSettingsWithDefaultValue() {
        final Map<String, String> appSettings = functionAppConfig.appSettings();
        setDefaultAppSetting(appSettings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING);
        setDefaultAppSetting(appSettings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
                FUNCTIONS_EXTENSION_VERSION_VALUE, null);
        return appSettings;
    }

    private void setDefaultAppSetting(Map<String, String> result, String settingName, String settingIsEmptyMessage,
                                      String defaultValue, String warningMessage) {
        final String setting = result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            AzureMessager.getMessager().info(settingIsEmptyMessage);
            result.put(settingName, defaultValue);
            return;
        }
        // Show warning message when user set a different value
        if (!StringUtils.equalsIgnoreCase(setting, defaultValue) && StringUtils.isNotEmpty(warningMessage)) {
            AzureMessager.getMessager().warning(warningMessage);
        }
    }

    private AzureTask<FunctionApp> getUpdateFunctionAppTask(final FunctionApp functionApp) {
        final AzureString title = AzureString.format("Update function app({0})", functionAppConfig.appName());
        return new AzureTask<>(title, () -> {
            AzureMessager.getMessager().info(String.format(UPDATE_FUNCTION_APP, functionApp.name()));
            // update app settings
            final IAppServiceUpdater<? extends FunctionApp> update = functionApp.update();
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            if (functionAppConfig.disableAppInsights()) {
                update.withoutAppSettings(APPINSIGHTS_INSTRUMENTATION_KEY);
            } else if (applicationInsights != null) {
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, applicationInsights.getInstrumentationKey());
            }
            final FunctionApp result = update.withPlan(appServicePlan.id())
                    .withRuntime(getRuntime(functionAppConfig.runtime()))
                    .withDockerConfiguration(getDockerConfiguration(functionAppConfig.runtime()))
                    .withAppSettings(appSettings)
                    .commit();
            AzureMessager.getMessager().info(String.format(UPDATE_FUNCTION_DONE, functionApp.name()));
            return result;
        });
    }

    private AzureTask<FunctionAppDeploymentSlot> getCreateFunctionSlotTask(FunctionAppDeploymentSlot deploymentSlot) {
        final AzureString title = AzureString.format("Create new slot({0}) on function app ({1})",
                functionAppConfig.deploymentSlotName(), functionAppConfig.appName());
        return new AzureTask<>(title, () -> {
            AzureMessager.getMessager().info(FUNCTION_SLOT_CREATE_START);
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            Optional.ofNullable(applicationInsights).ifPresent(insights ->
                    appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, applicationInsights.getInstrumentationKey()));
            final FunctionAppDeploymentSlot result = deploymentSlot.create().withAppSettings(appSettings)
                    .withConfigurationSource(functionAppConfig.deploymentSlotConfigurationSource())
                    .withName(functionAppConfig.deploymentSlotName()).commit();
            AzureMessager.getMessager().info(String.format(FUNCTION_SLOT_CREATED, result.name()));
            return result;
        });
    }

    private AzureTask<FunctionAppDeploymentSlot> getUpdateFunctionSlotTask(FunctionAppDeploymentSlot deploymentSlot) {
        final AzureString title = AzureString.format("Update function deployment slot({0})", functionAppConfig.deploymentSlotName());
        return new AzureTask<>(title, () -> {
            AzureMessager.getMessager().info(FUNCTION_SLOT_UPDATE);
            final Map<String, String> appSettings = processAppSettingsWithDefaultValue();
            final FunctionAppDeploymentSlot.FunctionAppDeploymentSlotUpdater update = deploymentSlot.update();
            if (functionAppConfig.disableAppInsights()) {
                update.withoutAppSettings(APPINSIGHTS_INSTRUMENTATION_KEY);
            } else if (applicationInsights != null) {
                appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, applicationInsights.getInstrumentationKey());
            }
            final FunctionAppDeploymentSlot result = update.withAppSettings(appSettings).commit();
            AzureMessager.getMessager().info(String.format(FUNCTION_SLOT_UPDATE_DONE, result.name()));
            return deploymentSlot;
        });
    }

    private FunctionAppDeploymentSlot getFunctionDeploymentSlot(final FunctionApp functionApp) {
        if (!functionApp.exists()) {
            throw new AzureToolkitRuntimeException(FUNCTION_APP_NOT_EXIST_FOR_SLOT);
        }
        return functionApp.deploymentSlot(functionAppConfig.deploymentSlotName());
    }

    private AzureTask<ApplicationInsightsEntity> getApplicationInsightsTask() {
        return new AzureTask<>(() -> {
            try {
                final String name = StringUtils.firstNonEmpty(functionAppConfig.appInsightsInstance(), functionAppConfig.appName());
                return new GetOrCreateApplicationInsightsTask(functionAppConfig.subscriptionId(),
                        functionAppConfig.resourceGroup(), functionAppConfig.region(), name).getSupplier().get();
            } catch (final Exception e) {
                AzureMessager.getMessager().warning(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, e.getMessage()));
                return null;
            }
        });
    }

    private CreateResourceGroupTask getResourceGroupTask() {
        return new CreateResourceGroupTask(functionAppConfig.subscriptionId(), functionAppConfig.resourceGroup(), functionAppConfig.region());
    }

    private CreateOrUpdateAppServicePlanTask getServicePlanTask() {
        return new CreateOrUpdateAppServicePlanTask(functionAppConfig.getServicePlanConfig());
    }

    private Runtime getRuntime(RuntimeConfig runtime) {
        return Runtime.getRuntime(runtime.os(),
            WebContainer.JAVA_OFF,
            OperatingSystem.DOCKER != runtime.os() ? runtime.javaVersion() : JavaVersion.OFF);
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
    public IFunctionAppBase<?> execute() {
        this.tasks.forEach(t -> t.getSupplier().get());
        return functionApp;
    }
}
