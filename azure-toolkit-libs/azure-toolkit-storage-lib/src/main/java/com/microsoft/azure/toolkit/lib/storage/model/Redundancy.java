/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.toolkit.lib.common.model.ExpandableParameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
@EqualsAndHashCode
public class Redundancy implements ExpandableParameter {

    public static final Redundancy STANDARD_ZRS = new Redundancy(Performance.STANDARD, "Standard_ZRS", "Zone-Redundant");
    public static final Redundancy STANDARD_LRS = new Redundancy(Performance.STANDARD, "Standard_LRS", "Locally Redundant");
    public static final Redundancy STANDARD_GRS = new Redundancy(Performance.STANDARD, "Standard_GRS", "Geo-Redundant");
    public static final Redundancy STANDARD_RAGZRS = new Redundancy(Performance.STANDARD, "Standard_RAGZRS", "Read Access Geo-Zone-Redundant");
    public static final Redundancy PREMIUM_LRS = new Redundancy(Performance.PREMIUM, "Premium_LRS", "Locally Redundant");
    public static final Redundancy PREMIUM_ZRS = new Redundancy(Performance.PREMIUM, "Premium_ZRS", "Zone-Redundant");

    private static final List<Redundancy> values = new ImmutableList.Builder<Redundancy>().add(STANDARD_ZRS, STANDARD_LRS, STANDARD_GRS, STANDARD_RAGZRS, PREMIUM_LRS, PREMIUM_ZRS).build();

    private Performance performance;
    private String name;
    private String label;

    private Redundancy(Performance performance, String name, String label) {
        this.performance = performance;
        this.name = name;
        this.label = label;
    }

    public static List<Redundancy> values() {
        return values;
    }

    public static Redundancy fromName(@Nonnull String value) {
        return values().stream()
                .filter(region -> StringUtils.equalsAnyIgnoreCase(value, region.name, region.label))
                .findFirst().orElse(new Redundancy(Performance.STANDARD, value, value));
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }

}
