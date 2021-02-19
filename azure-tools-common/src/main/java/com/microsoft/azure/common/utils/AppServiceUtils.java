/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.utils;

import com.microsoft.azure.common.appservice.DockerImageType;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class AppServiceUtils {

    private static final String SERVICE_PLAN_NOT_FOUND = "Failed to get App Service Plan.";
    private static final String UPDATE_APP_SERVICE_PLAN = "Updating App Service Plan...";
    private static final List<PricingTier> pricingTiers = new ArrayList<>();

    static {
        // Init runtimeStack list
        for (final Field field : PricingTier.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    pricingTiers.add((PricingTier) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static AppServicePlan getAppServicePlan(final String servicePlanName, final Azure azureClient,
                                                   final String resourceGroup, final String servicePlanResourceGroup) {
        if (StringUtils.isNotEmpty(servicePlanName)) {
            final String servicePlanResGrp = getAppServicePlanResourceGroup(resourceGroup, servicePlanResourceGroup);
            return azureClient.appServices().appServicePlans()
                .getByResourceGroup(servicePlanResGrp, servicePlanName);
        }
        return null;
    }

    public static String getAppServicePlanResourceGroup(final String resourceGroup, final String appServicePlanResGrp) {
        return StringUtils.isEmpty(appServicePlanResGrp) ? resourceGroup : appServicePlanResGrp;
    }

    public static String getAppServicePlanName(final String servicePlanName) {
        return StringUtils.isEmpty(servicePlanName) ? generateRandomServicePlanName() : servicePlanName;
    }

    private static String generateRandomServicePlanName() {
        return "ServicePlan" + UUID.randomUUID().toString().substring(0, 18);
    }

    public static PricingTier getPricingTierFromString(final String pricingTierString) {
        for (final PricingTier pricingTier : pricingTiers) {
            if (pricingTier.toSkuDescription().size().equalsIgnoreCase(pricingTierString)) {
                return pricingTier;
            }
        }
        return null;
    }

    public static String convertPricingTierToString(final PricingTier pricingTier) {
        return pricingTier == null ? null : pricingTier.toSkuDescription().size();
    }

    public static List<PricingTier> getAvailablePricingTiers(OperatingSystem operatingSystem) {
        // This is a workaround for https://github.com/Azure/azure-libraries-for-java/issues/660
        // Linux app service didn't support P1,P2,P3 pricing tier.
        final List<PricingTier> result = new ArrayList<>(pricingTiers);
        if (operatingSystem == OperatingSystem.LINUX) {
            result.remove(PricingTier.PREMIUM_P1);
            result.remove(PricingTier.PREMIUM_P2);
            result.remove(PricingTier.PREMIUM_P3);
        }
        return result;
    }

    public static AppServicePlan getAppServicePlanByAppService(final WebAppBase webApp) {
        return webApp.manager().appServicePlans().getById(webApp.appServicePlanId());
    }

    public static AppServicePlan updateAppServicePlan(final AppServicePlan appServicePlan,
                                                      final PricingTier pricingTier) throws AzureExecutionException {
        if (appServicePlan == null) {
            throw new AzureExecutionException(SERVICE_PLAN_NOT_FOUND);
        }
        // Return when no need to update
        if (pricingTier == null || pricingTier.equals(appServicePlan.pricingTier())) {
            return appServicePlan;
        }
        Log.prompt(UPDATE_APP_SERVICE_PLAN);
        final AppServicePlan.Update appServicePlanUpdate = appServicePlan.update();
        return appServicePlanUpdate.withPricingTier(pricingTier).apply();
    }

    public static boolean isEqualAppServicePlan(AppServicePlan first, AppServicePlan second) {
        return first == null ? second == null : second != null && StringUtils.equals(first.id(), second.id());
    }

    public static DockerImageType getDockerImageType(final String imageName, final boolean hasCredential,
                                                     final String registryUrl) {
        if (StringUtils.isEmpty(imageName)) {
            return DockerImageType.NONE;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(registryUrl);

        if (isCustomRegistry) {
            return hasCredential ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return hasCredential ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }

    public static RuntimeStack parseRuntimeStack(String linuxFxVersion) {
        if (StringUtils.isEmpty(linuxFxVersion)) {
            return null;
        }
        final String[] segments = linuxFxVersion.split(Pattern.quote("|"));
        if (segments.length != 2) {
            return null;
        }
        return new RuntimeStack(segments[0], segments[1]);
    }

    public static boolean isDockerAppService(WebAppBase webapp) {
        final String linuxFxVersion = webapp.linuxFxVersion();
        return StringUtils.containsIgnoreCase(linuxFxVersion, "DOCKER|");
    }

}
