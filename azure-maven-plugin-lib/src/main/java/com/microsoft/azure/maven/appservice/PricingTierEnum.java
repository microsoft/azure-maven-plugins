/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.appservice;

import com.microsoft.azure.management.appservice.PricingTier;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.maven.plugin.MojoExecutionException;

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
    P1V2("P1V2"),
    p1v2("P1V2"),
    p1V2("P1V2"),
    P1v2("P1V2"),
    P2V2("P2V2"),
    P2v2("P2V2"),
    p2V2("P2V2"),
    p2v2("P2V2"),
    P3V2("P3V2"),
    p3V2("P3V2"),
    P3v2("P3V2"),
    p3v2("P3V2");

    private final String pricingTier;
    private static final BidiMap<String, PricingTier> pricingTierBidiMap = new DualHashBidiMap<>();

    static {
        pricingTierBidiMap.put("F1", PricingTier.FREE_F1);
        pricingTierBidiMap.put("D1", PricingTier.SHARED_D1);
        pricingTierBidiMap.put("B1", PricingTier.BASIC_B1);
        pricingTierBidiMap.put("B2", PricingTier.BASIC_B2);
        pricingTierBidiMap.put("B3", PricingTier.BASIC_B3);
        pricingTierBidiMap.put("S1", PricingTier.STANDARD_S1);
        pricingTierBidiMap.put("S2", PricingTier.STANDARD_S2);
        pricingTierBidiMap.put("S3", PricingTier.STANDARD_S3);
        pricingTierBidiMap.put("P1V2", new PricingTier("Premium", "P1V2"));
        pricingTierBidiMap.put("P2V2", new PricingTier("Premium", "P2V2"));
        pricingTierBidiMap.put("P3V2", new PricingTier("Premium", "P3V2"));
    }

    PricingTierEnum(final String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public PricingTier toPricingTier() throws MojoExecutionException {
        if (pricingTierBidiMap.containsKey(pricingTier)) {
            return pricingTierBidiMap.get(pricingTier);
        } else {
            throw new MojoExecutionException("Unknown value of the pricingTier, please correct it in pom.xml.");
        }
    }

    public static String getPricingTierStringByPricingTierObject(PricingTier pricingTier) {
        return pricingTierBidiMap.getKey(pricingTier);
    }
}
