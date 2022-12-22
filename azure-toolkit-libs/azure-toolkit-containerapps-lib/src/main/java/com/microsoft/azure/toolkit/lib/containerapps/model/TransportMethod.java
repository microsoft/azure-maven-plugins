/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class TransportMethod {
    public static final TransportMethod AUTO = new TransportMethod("auto");
    public static final TransportMethod HTTP = new TransportMethod("http");
    public static final TransportMethod HTTP2 = new TransportMethod("http2");
    public static final TransportMethod TCP = new TransportMethod("tcp");

    private String value;

    public static List<TransportMethod> values() {
        return Arrays.asList(AUTO, HTTP, HTTP2, TCP);
    }

    public static TransportMethod fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst()
                .orElse(null);
    }
}
