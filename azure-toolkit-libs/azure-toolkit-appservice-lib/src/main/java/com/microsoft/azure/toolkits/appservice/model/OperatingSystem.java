/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkits.appservice.model;

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

    public static OperatingSystem getFromServiceModel(com.azure.resourcemanager.appservice.models.OperatingSystem operatingSystem) {
        return Arrays.stream(OperatingSystem.values())
                .filter(os -> StringUtils.equals(operatingSystem.name(), os.getValue()))
                .findFirst().orElse(null);
    }
}
