/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

import com.azure.resourcemanager.appservice.models.SkuDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public class PricingTier {
    public static final PricingTier BASIC_B1 = new PricingTier("Basic", "B1");
    public static final PricingTier BASIC_B2 = new PricingTier("Basic", "B2");
    public static final PricingTier BASIC_B3 = new PricingTier("Basic", "B3");
    public static final PricingTier STANDARD_S1 = new PricingTier("Standard", "S1");
    public static final PricingTier STANDARD_S2 = new PricingTier("Standard", "S2");
    public static final PricingTier STANDARD_S3 = new PricingTier("Standard", "S3");
    public static final PricingTier PREMIUM_P1 = new PricingTier("Premium", "P1");
    public static final PricingTier PREMIUM_P2 = new PricingTier("Premium", "P2");
    public static final PricingTier PREMIUM_P3 = new PricingTier("Premium", "P3");
    public static final PricingTier PREMIUM_P1V2 = new PricingTier("PremiumV2", "P1v2");
    public static final PricingTier PREMIUM_P2V2 = new PricingTier("PremiumV2", "P2v2");
    public static final PricingTier PREMIUM_P3V2 = new PricingTier("PremiumV2", "P3v2");
    public static final PricingTier PREMIUM_P1V3 = new PricingTier("PremiumV3", "P1v3");
    public static final PricingTier PREMIUM_P2V3 = new PricingTier("PremiumV3", "P2v3");
    public static final PricingTier PREMIUM_P3V3 = new PricingTier("PremiumV3", "P3v3");
    public static final PricingTier FREE_F1 = new PricingTier("Free", "F1");
    public static final PricingTier SHARED_D1 = new PricingTier("Shared", "D1");

    private String tier;
    private String size;

    public static List<PricingTier> values() {
        return Arrays.asList(BASIC_B1, BASIC_B2, BASIC_B3, STANDARD_S1, STANDARD_S2, STANDARD_S3, PREMIUM_P1, PREMIUM_P2, PREMIUM_P3, PREMIUM_P1V2,
                PREMIUM_P2V2, PREMIUM_P3V2, PREMIUM_P1V3, PREMIUM_P2V3, PREMIUM_P3V3, FREE_F1, SHARED_D1);
    }

    public static com.azure.resourcemanager.appservice.models.PricingTier convertToServiceModel(PricingTier pricingTier) {
        final SkuDescription skuDescription = new SkuDescription().withTier(pricingTier.getTier()).withSize(pricingTier.getSize());
        return com.azure.resourcemanager.appservice.models.PricingTier.fromSkuDescription(skuDescription);
    }

    public static PricingTier createFromServiceModel(com.azure.resourcemanager.appservice.models.PricingTier pricingTier) {
        return values().stream()
                .filter(value -> StringUtils.equals(value.getSize(), pricingTier.toSkuDescription().size()) &&
                        StringUtils.equals(value.getTier(), pricingTier.toSkuDescription().tier()))
                .findFirst().orElse(null);
    }
}
