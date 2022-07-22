/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LoadBalancerSku {
    public static final LoadBalancerSku STANDARD = new LoadBalancerSku("standard");
    public static final LoadBalancerSku BASIC = new LoadBalancerSku("basic");

    private String value;

    public static List<LoadBalancerSku> values() {
        return Arrays.asList(STANDARD, BASIC);
    }

    public static LoadBalancerSku fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
