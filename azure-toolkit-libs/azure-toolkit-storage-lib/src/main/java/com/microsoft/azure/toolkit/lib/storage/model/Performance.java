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
public class Performance implements ExpandableParameter {

    public static final Performance STANDARD = new Performance("Standard", "Recommended for most scenarios (general-purpose v2 account)");
    public static final Performance PREMIUM = new Performance("Premium", "Recommended for scenarios that require low latency");

    private static final List<Performance> values = new ImmutableList.Builder<Performance>().add(STANDARD, PREMIUM).build();

    private final String name;
    private final String label;

    private Performance(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public static List<Performance> values() {
        return values;
    }

    public static Performance fromName(@Nonnull String value) {
        return values().stream()
                .filter(region -> StringUtils.equalsAnyIgnoreCase(value, region.name, region.label))
                .findFirst().orElse(new Performance(value, value));
    }

    @Override
    public boolean isExpandedValue() {
        return !values().contains(this);
    }

}
