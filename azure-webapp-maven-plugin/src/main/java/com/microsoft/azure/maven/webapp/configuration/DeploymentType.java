/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    NONE,
    FTP,
    WAR,
    JAR,
    UNKNOWN;

    public static DeploymentType fromString(final String input) {
        if (StringUtils.isEmpty(input)) {
            return NONE;
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            case "WAR":
                return WAR;
            case "JAR":
                return JAR;
            default:
                return UNKNOWN;
        }
    }
}
