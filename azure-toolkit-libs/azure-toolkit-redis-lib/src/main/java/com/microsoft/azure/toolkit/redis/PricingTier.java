/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.redis;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PricingTier {
    private static final String BASIC = "Basic";
    private static final String STANDARD = "Standard";
    private static final String PREMIUM = "Premium";
    private String family;
    private String capacity;
    private String memory;
    private boolean replication;

    public static final PricingTier BASIC_C0 = new PricingTier(BASIC, "C0", "250MB", false);
    public static final PricingTier BASIC_C1 = new PricingTier(BASIC, "C1", "1GB", false);
    public static final PricingTier BASIC_C2 = new PricingTier(BASIC, "C2", "2.5GB", false);
    public static final PricingTier BASIC_C3 = new PricingTier(BASIC, "C3", "6GB", false);
    public static final PricingTier BASIC_C4 = new PricingTier(BASIC, "C4", "13GB", false);
    public static final PricingTier BASIC_C5 = new PricingTier(BASIC, "C5", "26GB", false);
    public static final PricingTier BASIC_C6 = new PricingTier(BASIC, "C6", "53GB", false);

    public static final PricingTier STANDARD_C0 = new PricingTier(STANDARD, "C0", "250MB", true);
    public static final PricingTier STANDARD_C1 = new PricingTier(STANDARD, "C1", "1GB", true);
    public static final PricingTier STANDARD_C2 = new PricingTier(STANDARD, "C2", "2.5GB", true);
    public static final PricingTier STANDARD_C3 = new PricingTier(STANDARD, "C3", "6GB", true);
    public static final PricingTier STANDARD_C4 = new PricingTier(STANDARD, "C4", "13GB", true);
    public static final PricingTier STANDARD_C5 = new PricingTier(STANDARD, "C5", "26GB", true);
    public static final PricingTier STANDARD_C6 = new PricingTier(STANDARD, "C6", "53GB", true);

    public static final PricingTier PREMIUM_C1 = new PricingTier(PREMIUM, "C1", "6GB", true);
    public static final PricingTier PREMIUM_C2 = new PricingTier(PREMIUM, "C2", "13GB", true);
    public static final PricingTier PREMIUM_C3 = new PricingTier(PREMIUM, "C3", "26GB", true);
    public static final PricingTier PREMIUM_C4 = new PricingTier(PREMIUM, "C4", "53GB", true);
    public static final PricingTier PREMIUM_C5 = new PricingTier(PREMIUM, "C5", "120GB", true);

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
        return String.format("%s %s (%s)", family, capacity, replication ? (memory + ", Replication") : memory);
    }
}
