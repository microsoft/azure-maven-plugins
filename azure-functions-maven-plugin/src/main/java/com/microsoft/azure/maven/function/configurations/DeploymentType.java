/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.microsoft.azure.maven.appservice.DeploymentTypeValues;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

public enum DeploymentType {
    MS_DEPLOY(DeploymentTypeValues.MS_DEPLOY),
    ZIP(DeploymentTypeValues.ZIP),
    FTP(DeploymentTypeValues.FTP);

    private final String value;

    public static final String UNKNOWN_DEPLOYMENT_TYPE = String.format(
            "Unknown deployment type, supported values are: %s, %s and %s.", DeploymentTypeValues.ZIP,
            DeploymentTypeValues.FTP, DeploymentTypeValues.MS_DEPLOY);

    DeploymentType(final String value) {
        this.value = value;
    }

    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }

        switch (input.toUpperCase(Locale.ENGLISH)) {
            case DeploymentTypeValues.FTP:
                return FTP;
            case DeploymentTypeValues.MS_DEPLOY:
                return MS_DEPLOY;
            case DeploymentTypeValues.ZIP:
                return ZIP;
            default:
                throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
    }

    @Override
    public String toString() {
        return this.value;
    }
}
