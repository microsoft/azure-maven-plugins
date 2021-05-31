/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum OperatingSystem {
    WINDOWS("windows"),
    LINUX("linux"),
    DOCKER("docker");

    private String value;

    public static OperatingSystem fromString(String value) {
        return Arrays.stream(values()).filter(operatingSystem -> StringUtils.equalsIgnoreCase(operatingSystem.value, value))
            .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return StringUtils.capitalize(value);
    }
}
