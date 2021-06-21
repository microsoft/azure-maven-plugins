/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LogLevel {
    public static final LogLevel OFF = new LogLevel("Off");
    public static final LogLevel VERBOSE = new LogLevel("Verbose");
    public static final LogLevel INFORMATION = new LogLevel("Information");
    public static final LogLevel WARNING = new LogLevel("Warning");
    public static final LogLevel ERROR = new LogLevel("Error");

    private String value;

    public static List<LogLevel> values() {
        return Arrays.asList(OFF, VERBOSE, INFORMATION, WARNING, ERROR);
    }

    public static LogLevel fromString(String input) {
        return values().stream()
                .filter(logLevel -> StringUtils.equalsIgnoreCase(input, logLevel.getValue()))
                .findFirst().orElse(null);
    }
}
