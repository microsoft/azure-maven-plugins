/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.microsoft.azure.toolkit.lib.common.model.ExpandableParameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingTier implements ExpandableParameter {
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
    // functions only
    public static final PricingTier CONSUMPTION = new PricingTier("Dynamic", "Y1");
    public static final PricingTier ELASTIC_PREMIUM_EP1 = new PricingTier("ElasticPremium", "EP1");
    public static final PricingTier ELASTIC_PREMIUM_EP2 = new PricingTier("ElasticPremium", "EP2");
    public static final PricingTier ELASTIC_PREMIUM_EP3 = new PricingTier("ElasticPremium", "EP3");

    public static final List<PricingTier> SHARED_PRICING = Collections.unmodifiableList(Arrays.asList(BASIC_B1, BASIC_B2, BASIC_B3, STANDARD_S1, STANDARD_S2,
            STANDARD_S3, PREMIUM_P1, PREMIUM_P2, PREMIUM_P3, PREMIUM_P1V2, PREMIUM_P2V2, PREMIUM_P3V2, PREMIUM_P1V3, PREMIUM_P2V3, PREMIUM_P3V3,
            FREE_F1, SHARED_D1));
    public static final List<PricingTier> WEB_APP_PRICING = Collections.unmodifiableList(SHARED_PRICING);
    public static final List<PricingTier> FUNCTION_PRICING = Collections.unmodifiableList(ListUtils.union(SHARED_PRICING,
            Arrays.asList(CONSUMPTION, ELASTIC_PREMIUM_EP1, ELASTIC_PREMIUM_EP2, ELASTIC_PREMIUM_EP3)));
    private static final List<PricingTier> values =
            Collections.unmodifiableList(new ArrayList<>(new HashSet<>(ListUtils.union(WEB_APP_PRICING, FUNCTION_PRICING))));
    private static final String CONSUMPTION_SIZE = "consumption";

    private String tier;
    private String size;

    public static List<PricingTier> values() {
        return values;
    }

    public static PricingTier fromString(@Nonnull String size) {
        return fromString(null, size);
    }

    public static PricingTier fromString(@Nullable String tier, @Nonnull String size) {
        if (StringUtils.equalsIgnoreCase(CONSUMPTION_SIZE, size)) {
            return PricingTier.CONSUMPTION;
        }
        return values().stream()
                .filter(pricingTier -> StringUtils.equalsIgnoreCase(size, pricingTier.size) &&
                        (StringUtils.isEmpty(tier) || StringUtils.equals(tier, pricingTier.tier)))
                .findFirst().orElseGet(() -> new PricingTier(tier, size));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PricingTier)) {
            return false;
        }
        final PricingTier that = (PricingTier) o;
        return Objects.equals(tier, that.tier) && Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, size);
    }

    @Override
    public String toString() {
        if (this.equals(CONSUMPTION)) {
            return "Consumption";
        }
        return this.getSize();
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }
}
