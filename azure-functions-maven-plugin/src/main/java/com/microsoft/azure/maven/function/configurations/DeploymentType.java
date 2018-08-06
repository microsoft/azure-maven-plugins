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
    MS_DEPLOY(ConstantValues.MS_DEPLOY_VALUE),
    ZIP(ConstantValues.ZIP_VALUE),
    FTP(ConstantValues.FTP_VALUE);

    public static class ConstantValues {
        public static final String MS_DEPLOY_VALUE = "msdeploy";
        public static final String ZIP_VALUE = "zip";
        public static final String FTP_VALUE = "ftp";
    }

    private final String value;

    public static final String UNKNOWN_DEPLOYMENT_TYPE =
        "Unknown deployment type, supported values are: zip, ftp and msdeploy.";

    DeploymentType(final String value) {
        this.value = value;
    }

    public static DeploymentType fromString(final String input) throws MojoExecutionException {
        if (StringUtils.isEmpty(input)) {
            throw new MojoExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }

        switch (input.toLowerCase(Locale.ENGLISH)) {
            case ConstantValues.FTP_VALUE:
                return FTP;
            case ConstantValues.MS_DEPLOY_VALUE:
                return MS_DEPLOY;
            case ConstantValues.ZIP_VALUE:
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
