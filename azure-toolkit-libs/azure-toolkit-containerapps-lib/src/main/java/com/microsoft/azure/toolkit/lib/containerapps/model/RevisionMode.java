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
public class RevisionMode {
    public static final RevisionMode MULTIPLE = new RevisionMode("Multiple");
    public static final RevisionMode SINGLE = new RevisionMode("Single");
    public static final RevisionMode UNKNOWN = new RevisionMode("Unknown");

    private String value;

    public static List<RevisionMode> values() {
        return Arrays.asList(MULTIPLE, SINGLE);
    }

    public static RevisionMode fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(UNKNOWN);
    }
}
