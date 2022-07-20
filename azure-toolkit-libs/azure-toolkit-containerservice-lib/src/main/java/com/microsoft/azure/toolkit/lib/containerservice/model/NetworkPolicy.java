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
public class NetworkPolicy {
    public static final NetworkPolicy CALICO = new NetworkPolicy("calico");
    public static final NetworkPolicy AZURE = new NetworkPolicy("azure");

    private String value;

    public static List<NetworkPolicy> values() {
        return Arrays.asList(CALICO, AZURE);
    }

    public static NetworkPolicy fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
