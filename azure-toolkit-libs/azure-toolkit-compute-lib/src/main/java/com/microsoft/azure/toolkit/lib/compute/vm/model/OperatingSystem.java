/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.vm.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum OperatingSystem {
    Windows,
    Linux;

    public static OperatingSystem fromString(String value) {
        return Arrays.stream(values()).filter(operatingSystem -> StringUtils.equalsIgnoreCase(operatingSystem.name(), value))
                .findFirst().orElse(null);
    }
}
