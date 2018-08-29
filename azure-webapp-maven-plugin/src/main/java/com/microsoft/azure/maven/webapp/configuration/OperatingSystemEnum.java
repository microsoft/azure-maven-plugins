/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum OperatingSystemEnum {
    Windows,
    Linux,
    Docker;

    public static OperatingSystemEnum fromString(final String input) throws MojoExecutionException {
        final String errorMessage = "Unknown value of <os> configured in pom.xml.";
        if (StringUtils.isEmpty(input)) {
            throw new MojoExecutionException(errorMessage);
        }
        switch (input.toLowerCase(Locale.ENGLISH)) {
            case "windows":
                return Windows;
            case "linux":
                return Linux;
            case "docker":
                return Docker;
            default:
                throw new MojoExecutionException(errorMessage);
        }
    }
}
