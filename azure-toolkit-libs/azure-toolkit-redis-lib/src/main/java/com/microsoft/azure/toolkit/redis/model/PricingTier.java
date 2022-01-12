/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis.model;

import com.azure.resourcemanager.redis.models.Sku;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PricingTier {
    private static final String BASIC = "Basic";
    private static final String STANDARD = "Standard";
    private static final String PREMIUM = "Premium";

    public static final PricingTier BASIC_C0 = new PricingTier(BASIC, "C0");
    public static final PricingTier BASIC_C1 = new PricingTier(BASIC, "C1");
    public static final PricingTier BASIC_C2 = new PricingTier(BASIC, "C2");
    public static final PricingTier BASIC_C3 = new PricingTier(BASIC, "C3");
    public static final PricingTier BASIC_C4 = new PricingTier(BASIC, "C4");
    public static final PricingTier BASIC_C5 = new PricingTier(BASIC, "C5");
    public static final PricingTier BASIC_C6 = new PricingTier(BASIC, "C6");

    public static final PricingTier STANDARD_C0 = new PricingTier(STANDARD, "C0");
    public static final PricingTier STANDARD_C1 = new PricingTier(STANDARD, "C1");
    public static final PricingTier STANDARD_C2 = new PricingTier(STANDARD, "C2");
    public static final PricingTier STANDARD_C3 = new PricingTier(STANDARD, "C3");
    public static final PricingTier STANDARD_C4 = new PricingTier(STANDARD, "C4");
    public static final PricingTier STANDARD_C5 = new PricingTier(STANDARD, "C5");
    public static final PricingTier STANDARD_C6 = new PricingTier(STANDARD, "C6");

    public static final PricingTier PREMIUM_C1 = new PricingTier(PREMIUM, "P1");
    public static final PricingTier PREMIUM_C2 = new PricingTier(PREMIUM, "P2");
    public static final PricingTier PREMIUM_C3 = new PricingTier(PREMIUM, "P3");
    public static final PricingTier PREMIUM_C4 = new PricingTier(PREMIUM, "P4");
    public static final PricingTier PREMIUM_C5 = new PricingTier(PREMIUM, "P5");

    private final String family;
    private final String capacity;

    public static PricingTier from(Sku sku) {
        final String name = sku.name().toString();
        final String family = sku.family().toString();
        return new PricingTier(name, family + sku.capacity());
    }

    public boolean isBasic() {
        return StringUtils.equals(family, BASIC);
    }

    public boolean isStandard() {
        return StringUtils.equals(family, STANDARD);
    }

    public boolean isPremium() {
        return StringUtils.equals(family, PREMIUM);
    }

    public int getSize() {
        return Integer.parseInt(this.capacity.substring(1));
    }

    private static final List<PricingTier> values = new ImmutableList.Builder<PricingTier>().add(
        BASIC_C0, BASIC_C1, BASIC_C2, BASIC_C3, BASIC_C4, BASIC_C5, BASIC_C6,
        STANDARD_C0, STANDARD_C1, STANDARD_C2, STANDARD_C3, STANDARD_C4, STANDARD_C5, STANDARD_C6,
        PREMIUM_C1, PREMIUM_C2, PREMIUM_C3, PREMIUM_C4
    ).build();

    public static List<PricingTier> values() {
        return values;
    }

    @Override
    public String toString() {
        return String.format("%s %s", family, capacity);
    }
}
