/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.utils.Utils.copyProperties;
import static com.microsoft.azure.toolkit.lib.common.utils.Utils.selectFirstOptionIfCurrentInvalid;

public class AppServiceConfigUtils {

    public static FunctionAppConfig fromFunctionApp(@Nonnull FunctionAppBase<?, ?, ?> app) {
        return app instanceof FunctionApp && StringUtils.isNotBlank(((FunctionApp) app).getEnvironmentId()) ?
            fromFunctionApp((FunctionApp) app, Objects.requireNonNull(((FunctionApp) app).getEnvironment())) :
            fromFunctionApp(app, Objects.requireNonNull(app.getAppServicePlan()));
    }

    public static FunctionAppConfig fromFunctionApp(@Nonnull FunctionAppBase<?, ?, ?> app, @Nonnull AppServicePlan servicePlan) {
        final FunctionAppConfig result = new FunctionAppConfig();
        fromAppService(app, servicePlan, result);
        // todo merge storage account configurations
        // todo merge application insights configurations
        final FlexConsumptionConfiguration flexConsumptionConfiguration = app.getFlexConsumptionConfiguration();
        Optional.ofNullable(flexConsumptionConfiguration).ifPresent(result::flexConsumptionConfiguration);
        return result;
    }

    public static FunctionAppConfig fromFunctionApp(@Nonnull FunctionApp app, @Nonnull ContainerAppsEnvironment environment) {
        final FunctionAppConfig result = new FunctionAppConfig();
        fromAppService(app, null, result);
        // todo merge storage account configurations
        // todo merge application insights configurations
        result.environment(environment.getName());
        result.containerConfiguration(app.getContainerConfiguration());
        return result;
    }

    public static AppServiceConfig fromAppService(@Nonnull AppServiceAppBase<?, ?, ?> app) {
        return app instanceof FunctionApp ? fromFunctionApp((FunctionApp) app) : fromAppService(app, app.getAppServicePlan(), new AppServiceConfig());
    }

    public static AppServiceConfig fromAppService(@Nonnull AppServiceAppBase<?, ?, ?> app, @Nonnull AppServicePlan servicePlan) {
        return fromAppService(app, servicePlan, new AppServiceConfig());
    }

    public static AppServiceConfig fromAppService(@Nonnull AppServiceAppBase<?, ?, ?> app, @Nullable AppServicePlan servicePlan, @Nonnull AppServiceConfig config) {
        config.appName(app.getName());

        config.resourceGroup(app.getResourceGroupName());
        config.subscriptionId(app.getSubscriptionId());
        config.region(app.getRegion());
        final Runtime runtime = app.getRuntime();

        final RuntimeConfig runtimeConfig;
        if (runtime != null && runtime.isDocker()) {
            runtimeConfig = app.getDockerRuntimeConfig();
        } else {
            runtimeConfig = new RuntimeConfig();
            Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).ifPresent(runtimeConfig::os);
            Optional.ofNullable(runtime).map(Runtime::getJavaVersionUserText).ifPresent(runtimeConfig::javaVersion);
            Optional.ofNullable(runtime).filter(r -> r instanceof WebAppRuntime).map(r -> ((WebAppRuntime) r))
                .map(WebAppRuntime::getContainerUserText).ifPresent(runtimeConfig::webContainer);
        }
        config.runtime(runtimeConfig);
        Optional.ofNullable(servicePlan).map(AppServicePlan::getPricingTier).ifPresent(config::pricingTier);
        Optional.ofNullable(servicePlan).map(AppServicePlan::getName).ifPresent(config::servicePlanName);
        Optional.ofNullable(servicePlan).map(AppServicePlan::getResourceGroupName).ifPresent(config::servicePlanResourceGroup);
        return config;
    }

    public static AppServiceConfig buildDefaultWebAppConfig(String subscriptionId, String resourceGroup, String appName, String packaging) {
        final AppServiceConfig appServiceConfig = AppServiceConfig.buildDefaultWebAppConfig(resourceGroup, appName, packaging);
        final List<Region> regions = Azure.az(AzureAppService.class).forSubscription(subscriptionId).listSupportedRegions();
        // replace with first region when the default region is not present
        appServiceConfig.region(selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        return appServiceConfig;
    }

    public static void mergeAppServiceConfig(AppServiceConfig to, AppServiceConfig from) {
        try {
            copyProperties(to, from, true);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("Cannot copy object for class AppServiceConfig.", e);
        }

        if (to.runtime() != from.runtime()) {
            mergeRuntime(to.runtime(), from.runtime());
        }
    }

    private static void mergeRuntime(RuntimeConfig to, RuntimeConfig from) {
        try {
            copyProperties(to, from, true);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("Cannot copy object for class RuntimeConfig.", e);
        }
    }
}
