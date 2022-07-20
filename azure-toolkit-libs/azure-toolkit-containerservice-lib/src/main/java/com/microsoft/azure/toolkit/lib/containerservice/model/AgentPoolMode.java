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
public class AgentPoolMode {
    public static final AgentPoolMode SYSTEM = new AgentPoolMode("System");
    public static final AgentPoolMode USER = new AgentPoolMode("User");

    private String value;

    public static List<AgentPoolMode> values() {
        return Arrays.asList(SYSTEM, USER);
    }

    public static AgentPoolMode fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
