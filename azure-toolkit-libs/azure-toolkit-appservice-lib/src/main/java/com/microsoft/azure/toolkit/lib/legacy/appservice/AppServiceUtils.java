/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.appservice;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AppServiceUtils {
    public static PricingTier getPricingTierFromString(final String pricingTierString) {
        return PricingTier.fromString(pricingTierString);
    }

    public static String convertPricingTierToString(final PricingTier pricingTier) {
        return pricingTier == null ? null : pricingTier.getSize();
    }

    public static List<PricingTier> getAvailablePricingTiers(OperatingSystem operatingSystem) {
        // This is a workaround for https://github.com/Azure/azure-libraries-for-java/issues/660
        // Linux app service didn't support P1,P2,P3 pricing tier.
        final List<PricingTier> result = new ArrayList<>(PricingTier.values());
        if (operatingSystem == OperatingSystem.LINUX || operatingSystem == OperatingSystem.DOCKER) {
            result.remove(PricingTier.PREMIUM_P1);
            result.remove(PricingTier.PREMIUM_P2);
            result.remove(PricingTier.PREMIUM_P3);
        }
        return result;
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

    public static boolean isDockerAppService(IWebApp webapp) {
        return webapp != null && webapp.getRuntime() != null &&
            Objects.equals(webapp.getRuntime().getOperatingSystem(), OperatingSystem.DOCKER);
    }

}
