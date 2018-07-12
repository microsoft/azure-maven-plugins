/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum ConfigurationSourceType {
    NEW,
    PARENT,
    OTHERS;

    public static ConfigurationSourceType fromString(String input) {
        if (StringUtils.isEmpty(input)) {
            return PARENT;
        }
        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "NEW":
                return NEW;
            case "PARENT":
                return PARENT;
            default:
                return OTHERS;
        }
    }
}
