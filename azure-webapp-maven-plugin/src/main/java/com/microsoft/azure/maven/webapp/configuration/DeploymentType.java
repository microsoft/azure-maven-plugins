/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    FTP,
    LOCAL_GIT;

    public static final String DEPLOYMENT_TYPE_NOT_SUPPORTED = "Deployment type not supported: ";
    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            return FTP;
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            default:
                throw new MojoExecutionException(DEPLOYMENT_TYPE_NOT_SUPPORTED + input);
        }
    }
}
