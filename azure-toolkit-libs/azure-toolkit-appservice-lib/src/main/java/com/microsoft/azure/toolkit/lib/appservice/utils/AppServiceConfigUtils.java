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
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class AppServiceConfigUtils {
    private static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    private static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";

    public static AppServiceConfig getAppServiceConfigFromExisting(IWebApp webapp) {
        IAppServicePlan servicePlan = webapp.plan();
        AppServiceConfig config = new AppServiceConfig();
        config.appName(webapp.name());

        config.resourceGroup(webapp.entity().getResourceGroup());
        config.subscriptionId(Utils.getSubscriptionId(webapp.id()));
        config.region(webapp.entity().getRegion());
        config.pricingTier(servicePlan.entity().getPricingTier());
        RuntimeConfig runtimeConfig = new RuntimeConfig();
        if (AppServiceUtils.isDockerAppService(webapp)) {
            runtimeConfig.os(OperatingSystem.DOCKER);
            final Map<String, String> settings = webapp.entity().getAppSettings();

            final String imageSetting = settings.get(SETTING_DOCKER_IMAGE);
            if (StringUtils.isNotBlank(imageSetting)) {
                runtimeConfig.image(imageSetting);
            } else {
                runtimeConfig.image(webapp.entity().getDockerImageName());
            }
            final String registryServerSetting = settings.get(SETTING_REGISTRY_SERVER);
            if (StringUtils.isNotBlank(registryServerSetting)) {
                runtimeConfig.registryUrl(registryServerSetting);
            }
        } else {
            runtimeConfig.os(webapp.getRuntime().getOperatingSystem());
            runtimeConfig.webContainer(webapp.getRuntime().getWebContainer());
            runtimeConfig.javaVersion(webapp.getRuntime().getJavaVersion());
        }
        config.runtime(runtimeConfig);
        if (servicePlan.entity() != null) {
            config.pricingTier(servicePlan.entity().getPricingTier());
            config.servicePlanName(servicePlan.name());
            config.servicePlanResourceGroup(servicePlan.entity().getResourceGroup());
        }
        return config;
    }

    public static AppServiceConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName, String packaging, JavaVersion javaVersion) {
        final AppServiceConfig appServiceConfig = AppServiceConfig.buildDefaultConfig(resourceGroup, appName, packaging, javaVersion);
        final List<Region> regions = Azure.az(AzureAppService.class).listSupportedRegions(subscriptionId);
        // replace with first region when the default region is not present
        appServiceConfig.region(Utils.selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        return appServiceConfig;
    }

    public static void mergeAppServiceConfig(AppServiceConfig config1, AppServiceConfig config2) {
        if (config1.region() == null) {
            config1.region(config2.region());
        }

        if (config1.servicePlanResourceGroup() == null) {
            config1.servicePlanResourceGroup(config2.servicePlanResourceGroup());
        }

        if (config1.servicePlanName() == null) {
            config1.servicePlanName(config2.servicePlanName());
        }

        if (config1.runtime() == null) {
            config1.runtime(config2.runtime());
        } else {
            mergeRuntime(config1.runtime(), config2.runtime());
        }

        if (config1.pricingTier() == null) {
            config1.pricingTier(config2.pricingTier());
        }
    }

    private static void mergeRuntime(RuntimeConfig runtime1, RuntimeConfig runtime2) {
        runtime1.os(ObjectUtils.firstNonNull(runtime1.os(), runtime2.os()));
        runtime1.image(ObjectUtils.firstNonNull(runtime1.image(), runtime2.image()));
        runtime1.username(ObjectUtils.firstNonNull(runtime1.username(), runtime2.username()));
        runtime1.password(ObjectUtils.firstNonNull(runtime1.password(), runtime2.password()));
        runtime1.startUpCommand(ObjectUtils.firstNonNull(runtime1.startUpCommand(), runtime2.startUpCommand()));
        runtime1.registryUrl(ObjectUtils.firstNonNull(runtime1.registryUrl(), runtime2.registryUrl()));
        runtime1.javaVersion(ObjectUtils.firstNonNull(runtime1.javaVersion(), runtime2.javaVersion()));
        runtime1.webContainer(ObjectUtils.firstNonNull(runtime1.webContainer(), runtime2.webContainer()));
    }
}
