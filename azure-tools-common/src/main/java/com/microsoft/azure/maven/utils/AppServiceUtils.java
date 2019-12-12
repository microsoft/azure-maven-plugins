/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.appservice.DockerImageType;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppServiceUtils {

    private static final String SERVICE_PLAN_NOT_FOUND = "Failed to get App Service Plan";
    private static final String UPDATE_APP_SERVICE_PLAN = "Updating app service plan";
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
                                                      final PricingTier pricingTier,
                                                      final Log log) throws AzureExecutionException {
        if (appServicePlan == null) {
            throw new AzureExecutionException(SERVICE_PLAN_NOT_FOUND);
        }
        log.info(String.format(UPDATE_APP_SERVICE_PLAN));
        final AppServicePlan.Update appServicePlanUpdate = appServicePlan.update();
        // Update pricing tier
        if (pricingTier != null && !appServicePlan.pricingTier().equals(pricingTier)) {
            appServicePlanUpdate.withPricingTier(pricingTier);
        }
        return appServicePlanUpdate.apply();
    }

    public static boolean isEqualAppServicePlan(AppServicePlan first, AppServicePlan second) {
        return first == null ? second == null : second != null && StringUtils.equals(first.id(), second.id());
    }

    public static DockerImageType getDockerImageType(final String imageName, final String serverId,
                                                     final String registryUrl) {
        if (StringUtils.isEmpty(imageName)) {
            return DockerImageType.NONE;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(registryUrl);
        final boolean isPrivate = StringUtils.isNotEmpty(serverId);

        if (isCustomRegistry) {
            return isPrivate ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return isPrivate ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }
}
