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
public class PowerState {
    public static final PowerState RUNNING = new PowerState("Running");
    public static final PowerState STOPPED = new PowerState("Stopped");

    private String value;

    public static List<PowerState> values() {
        return Arrays.asList(RUNNING, STOPPED);
    }

    public static PowerState fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
