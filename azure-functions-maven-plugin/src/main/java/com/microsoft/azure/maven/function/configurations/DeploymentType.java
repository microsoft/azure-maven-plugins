/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    MS_DEPLOY,
    ZIP,
    FTP;

    public static DeploymentType fromString(final String input) {
        if (StringUtils.isEmpty(input)) {
            return MS_DEPLOY;
        }
        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            case "ZIP":
                return ZIP;
            case "MSDEPLOY":
            default:
                return MS_DEPLOY;
        }
    }
}
