/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Accessors(fluent = true)
public class AppServiceConfig {

    private String subscriptionId;

    private String resourceGroup;

    private Region region;

    private PricingTier pricingTier;

    private String appName;

    private String servicePlanResourceGroup;

    private String servicePlanName;

    private RuntimeConfig runtime;

    private Map<String, String> appSettings;

    private Set<String> appSettingsToRemove;

    private String deploymentSlotName;

    private String deploymentSlotConfigurationSource;

    private DiagnosticConfig diagnosticConfig;

    public AppServicePlanConfig getServicePlanConfig() {
        return AppServicePlanConfig.builder()
            .subscriptionId(subscriptionId())
            .resourceGroupName(servicePlanResourceGroup())
            .name(servicePlanName())
            .region(region())
            .os(Optional.ofNullable(runtime()).map(RuntimeConfig::os).orElse(null))
            .pricingTier(pricingTier())
            .build();
    }

    public static AppServiceConfig buildDefaultWebAppConfig(String resourceGroup, String appName, String packaging) {
        final WebAppRuntime defaultRuntime = StringUtils.equalsIgnoreCase(packaging, "war") ? WebAppRuntime.getDefaultTomcatRuntime() :
            StringUtils.equalsIgnoreCase(packaging, "ear") ? WebAppRuntime.getDefaultJbossRuntime() : WebAppRuntime.getDefaultJavaseRuntime();
        final RuntimeConfig runtimeConfig = new RuntimeConfig()
            .os(defaultRuntime.getOperatingSystem())
            .webContainer(defaultRuntime.getContainerUserText())
            .javaVersion(defaultRuntime.getJavaVersionUserText());
        final AppServiceConfig appServiceConfig = buildDefaultAppServiceConfig(resourceGroup, appName);
        appServiceConfig.runtime(runtimeConfig);
        return appServiceConfig;
    }

    public static FunctionAppConfig buildDefaultFunctionConfig(String resourceGroup, String appName) {
        final FunctionAppConfig result = new FunctionAppConfig();
        final AppServiceConfig appServiceConfig = buildDefaultAppServiceConfig(resourceGroup, appName);
        AppServiceConfigUtils.mergeAppServiceConfig(result, appServiceConfig);
        final FunctionAppRuntime defaultRuntime = FunctionAppRuntime.getDefault();
        RuntimeConfig runtimeConfig = new RuntimeConfig()
            .os(defaultRuntime.getOperatingSystem())
            .javaVersion(defaultRuntime.getJavaVersionUserText());
        result.runtime(runtimeConfig);
        result.pricingTier(PricingTier.CONSUMPTION);
        result.flexConsumptionConfiguration(FlexConsumptionConfiguration.DEFAULT);
        return result;
    }

    @Nonnull
    private static AppServiceConfig buildDefaultAppServiceConfig(String resourceGroup, String appName) {
        final AppServiceConfig appServiceConfig = new AppServiceConfig();
        appServiceConfig.region(Region.US_CENTRAL);

        appServiceConfig.resourceGroup(resourceGroup);
        appServiceConfig.appName(appName);
        appServiceConfig.servicePlanResourceGroup(resourceGroup);
        appServiceConfig.servicePlanName(String.format("asp-%s", appName));
        return appServiceConfig;
    }
}
