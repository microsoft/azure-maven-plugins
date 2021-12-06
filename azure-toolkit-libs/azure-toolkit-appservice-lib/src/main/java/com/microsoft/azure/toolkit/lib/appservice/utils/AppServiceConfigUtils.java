/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.utils;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static com.microsoft.azure.toolkit.lib.common.utils.Utils.mergeObjects;
import static com.microsoft.azure.toolkit.lib.common.utils.Utils.selectFirstOptionIfCurrentInvalid;

public class AppServiceConfigUtils {
    private static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    private static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";

    public static AppServiceConfig fromAppService(IAppService<?> appService, AppServicePlan servicePlan) {
        AppServiceConfig config = new AppServiceConfig();
        config.appName(appService.name());

        config.resourceGroup(appService.entity().getResourceGroup());
        config.subscriptionId(Utils.getSubscriptionId(appService.id()));
        config.region(appService.entity().getRegion());
        config.pricingTier(servicePlan.entity().getPricingTier());
        RuntimeConfig runtimeConfig = new RuntimeConfig();
        if (AppServiceUtils.isDockerAppService(appService)) {
            runtimeConfig.os(OperatingSystem.DOCKER);
            final Map<String, String> settings = appService.entity().getAppSettings();

            final String imageSetting = settings.get(SETTING_DOCKER_IMAGE);
            if (StringUtils.isNotBlank(imageSetting)) {
                runtimeConfig.image(imageSetting);
            } else {
                runtimeConfig.image(appService.entity().getDockerImageName());
            }
            final String registryServerSetting = settings.get(SETTING_REGISTRY_SERVER);
            if (StringUtils.isNotBlank(registryServerSetting)) {
                runtimeConfig.registryUrl(registryServerSetting);
            }
        } else {
            runtimeConfig.os(appService.getRuntime().getOperatingSystem());
            runtimeConfig.webContainer(appService.getRuntime().getWebContainer());
            runtimeConfig.javaVersion(appService.getRuntime().getJavaVersion());
        }
        config.runtime(runtimeConfig);
        if (servicePlan.entity() != null) {
            config.pricingTier(servicePlan.entity().getPricingTier());
            config.servicePlanName(servicePlan.name());
            config.servicePlanResourceGroup(servicePlan.entity().getResourceGroup());
        }
        return config;
    }

    public static AppServiceConfig buildDefaultWebAppConfig(String subscriptionId, String resourceGroup, String appName, String packaging, JavaVersion javaVersion) {
        final AppServiceConfig appServiceConfig = AppServiceConfig.buildDefaultWebAppConfig(resourceGroup, appName, packaging, javaVersion);
        final List<Region> regions = Azure.az(AzureAppService.class).listSupportedRegions(subscriptionId);
        // replace with first region when the default region is not present
        appServiceConfig.region(selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        return appServiceConfig;
    }

    public static AppServiceConfig buildDefaultFunctionConfig(String subscriptionId, String resourceGroup, String appName, JavaVersion javaVersion) {
        final AppServiceConfig appServiceConfig = AppServiceConfig.buildDefaultFunctionConfig(resourceGroup, appName, javaVersion);
        final List<Region> regions = Azure.az(AzureAppService.class).listSupportedRegions(subscriptionId);
        // replace with first region when the default region is not present
        appServiceConfig.region(selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        return appServiceConfig;
    }

    public static void mergeAppServiceConfig(AppServiceConfig to, AppServiceConfig from) {
        try {
            mergeObjects(to, from);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("Cannot copy object for class AppServiceConfig.", e);
        }

        if (to.runtime() != from.runtime()) {
            mergeRuntime(to.runtime(), from.runtime());
        }
    }

    private static void mergeRuntime(RuntimeConfig to, RuntimeConfig from) {
        try {
            mergeObjects(to, from);
        } catch (IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("Cannot copy object for class RuntimeConfig.", e);
        }
    }
}
