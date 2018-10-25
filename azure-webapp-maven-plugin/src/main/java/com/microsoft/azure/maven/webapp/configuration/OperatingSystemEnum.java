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

    public static OperatingSystemEnum fromString(final String os) throws MojoExecutionException {
        if (StringUtils.isEmpty(os)) {
            throw new MojoExecutionException("The configuration <os> in <runtime> is required, " +
                "please specify it in pom.xml.");
        }
        switch (os.toLowerCase(Locale.ENGLISH)) {
            case "windows":
                return Windows;
            case "linux":
                return Linux;
            case "docker":
                return Docker;
            default:
                throw new MojoExecutionException("The value of <os> is unknown, supported values are: windows, " +
                    "linux and docker.");
        }
    }
}
