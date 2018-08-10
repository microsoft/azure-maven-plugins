/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    MS_DEPLOY,
    ZIP,
    FTP;

    public static final String UNKNOWN_DEPLOYMENT_TYPE =
        "The value of <deploymentType> is unknown, supported values are: ftp, zip and msdeploy.";

    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            case "MSDEPLOY":
                return MS_DEPLOY;
            case "ZIP":
                return ZIP;
            default:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
    }
}
