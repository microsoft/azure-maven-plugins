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
public class OsType {
    public static final OsType LINUX = new OsType("Linux");
    public static final OsType WINDOWS = new OsType("Windows");

    private String value;

    public static List<OsType> values() {
        return Arrays.asList(LINUX, WINDOWS);
    }

    public static OsType fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
