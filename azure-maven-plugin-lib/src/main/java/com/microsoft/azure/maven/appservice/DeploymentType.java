/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.appservice;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    FTP,
    ZIP,
    WAR,
    JAR,
    AUTO,
    NONE,
    EMPTY,
    MSDEPLOY,
    RUN_FROM_ZIP,
    RUN_FROM_BLOB;

    public static final String UNKNOWN_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown.";

    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            return EMPTY;
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "FTP":
                return FTP;
            case "MSDEPLOY":
                return MSDEPLOY;
            case "ZIP":
                return ZIP;
            case "WAR":
                return WAR;
            case "JAR":
                return JAR;
            case "AUTO":
                return AUTO;
            case "NONE":
                return NONE;
            case "RUN_FROM_ZIP":
                return RUN_FROM_ZIP;
            case "RUN_FROM_BLOB":
                return RUN_FROM_BLOB;
            default:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
    }
}
