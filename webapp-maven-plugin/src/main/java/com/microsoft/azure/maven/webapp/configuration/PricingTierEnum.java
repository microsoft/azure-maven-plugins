/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.management.appservice.PricingTier;

public enum PricingTierEnum {
    F1("F1"),
    f1("F1"),
    D1("D1"),
    d1("D1"),
    B1("B1"),
    b1("B1"),
    B2("B2"),
    b2("B2"),
    B3("B3"),
    b3("B3"),
    S1("S1"),
    s1("S1"),
    S2("S2"),
    s2("S2"),
    S3("S3"),
    s3("S3"),
    P1("P1"),
    p1("P1"),
    P2("P2"),
    p2("P2"),
    P3("P3"),
    p3("P3");

    private final String pricingTier;

    PricingTierEnum(final String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public PricingTier toPricingTier() {
        switch (pricingTier) {
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
            case "S2":
                return PricingTier.STANDARD_S2;
            case "S3":
                return PricingTier.STANDARD_S3;
            case "P1":
                return PricingTier.PREMIUM_P1;
            case "P2":
                return PricingTier.PREMIUM_P2;
            case "P3":
                return PricingTier.PREMIUM_P3;
            case "S1":
            default:
                return PricingTier.STANDARD_S1;
        }
    }
}
