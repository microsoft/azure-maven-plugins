/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class DeployType {
    public static final DeployType WAR = new DeployType("war");
    public static final DeployType JAR = new DeployType("jar");
    public static final DeployType EAR = new DeployType("ear");
    public static final DeployType JAR_LIB = new DeployType("lib");
    public static final DeployType STATIC = new DeployType("static");
    public static final DeployType SCRIPT_STARTUP = new DeployType("startup");
    public static final DeployType ZIP = new DeployType("zip");

    private final String value;

    public static List<DeployType> values() {
        return Arrays.asList(WAR, JAR, EAR, JAR_LIB, STATIC, SCRIPT_STARTUP, ZIP);
    }

    public static DeployType fromString(String input) {
        return values().stream()
            .filter(deployType -> StringUtils.equalsIgnoreCase(deployType.getValue(), input))
            .findFirst().orElse(null);
    }
}
