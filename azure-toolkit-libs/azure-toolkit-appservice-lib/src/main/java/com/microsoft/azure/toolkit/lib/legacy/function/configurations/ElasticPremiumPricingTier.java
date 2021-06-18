/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.configurations;

import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.management.appservice.SkuName;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum ElasticPremiumPricingTier {
    EP1,
    EP2,
    EP3;

    public PricingTier toPricingTier() {
        return new PricingTier(SkuName.ELASTIC_PREMIUM.toString(), this.name());
    }

    public static ElasticPremiumPricingTier fromString(String pricingTier) {
        return Arrays.stream(ElasticPremiumPricingTier.values())
                .filter(pricingTierEnum -> StringUtils.equalsIgnoreCase(pricingTier, pricingTierEnum.name()))
                .findFirst().orElse(null);
    }
}
