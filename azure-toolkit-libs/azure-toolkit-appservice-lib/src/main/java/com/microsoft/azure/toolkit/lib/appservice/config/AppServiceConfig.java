/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.config;

import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;

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

    private String deploymentSlotName;

    private String deploymentSlotConfigurationSource;

    public AppServicePlanConfig getServicePlanConfig() {
        return new AppServicePlanConfig()
            .subscriptionId(subscriptionId())
            .servicePlanResourceGroup(servicePlanResourceGroup())
            .servicePlanName(servicePlanName())
            .region(region())
            .os(runtime().os())
            .pricingTier(pricingTier());
    }

    public static AppServiceConfig buildDefaultWebAppConfig(String resourceGroup, String appName, String packaging, JavaVersion javaVersion) {
        RuntimeConfig runtimeConfig = new RuntimeConfig().os(OperatingSystem.LINUX).webContainer(StringUtils.equalsIgnoreCase(packaging, "war") ?
            WebContainer.TOMCAT_85 : (StringUtils.equalsIgnoreCase(packaging, "ear") ? WebContainer.JBOSS_7 : WebContainer.JAVA_SE))
            .javaVersion(javaVersion);
        AppServiceConfig appServiceConfig = buildCommonAppServiceConfig(resourceGroup, appName);
        appServiceConfig.runtime(runtimeConfig);
        return appServiceConfig;
    }

    public static AppServiceConfig buildDefaultFunctionConfig(String resourceGroup, String appName, JavaVersion javaVersion) {
        RuntimeConfig runtimeConfig = new RuntimeConfig().os(OperatingSystem.WINDOWS).webContainer(WebContainer.JAVA_OFF)
            .javaVersion(javaVersion);
        AppServiceConfig appServiceConfig = buildCommonAppServiceConfig(resourceGroup, appName);
        appServiceConfig.runtime(runtimeConfig);
        appServiceConfig.pricingTier(PricingTier.CONSUMPTION);
        return appServiceConfig;
    }

    @Nonnull
    private static AppServiceConfig buildCommonAppServiceConfig(String resourceGroup, String appName) {
        AppServiceConfig appServiceConfig = new AppServiceConfig();
        appServiceConfig.region(Region.US_CENTRAL);

        appServiceConfig.resourceGroup(resourceGroup);
        appServiceConfig.appName(appName);
        appServiceConfig.servicePlanResourceGroup(resourceGroup);
        appServiceConfig.servicePlanName(String.format("asp-%s", appName));
        return appServiceConfig;
    }
}
