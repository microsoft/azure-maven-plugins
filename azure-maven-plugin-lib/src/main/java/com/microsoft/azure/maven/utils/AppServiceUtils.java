/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.UUID;

public class AppServiceUtils {
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

    public static PricingTier getPricingTierFromString(final String value) throws MojoExecutionException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        switch (value.toUpperCase()) {
            case "F1":
                return PricingTier.FREE_F1;
            case "D1":
                return PricingTier.SHARED_D1;
            case "B1":
                return PricingTier.BASIC_B1;
            case "B2":
                return PricingTier.BASIC_B2;
            case "B3":
                return PricingTier.BASIC_B3;
            case "S1":
                return PricingTier.STANDARD_S1;
            case "S2":
                return PricingTier.STANDARD_S2;
            case "S3":
                return PricingTier.STANDARD_S3;
            // workaround to define the pricing tier with constructor since SDK has not updated the latest values
            // https://github.com/Azure/azure-libraries-for-java/issues/660
            case "P1V2":
                return new PricingTier("Premium", "P1V2");
            case "P2V2":
                return new PricingTier("Premium", "P2V2");
            case "P3V2":
                return new PricingTier("Premium", "P3V2");
            default:
                throw new MojoExecutionException("Unknown value of the pricingTier, please correct it in pom.xml.");
        }
    }
}
