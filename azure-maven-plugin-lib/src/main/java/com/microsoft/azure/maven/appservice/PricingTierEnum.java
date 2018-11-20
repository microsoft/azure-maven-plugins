/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.appservice;

import com.microsoft.azure.management.appservice.PricingTier;
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

    PricingTierEnum(final String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public PricingTier toPricingTier() throws MojoExecutionException {
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
