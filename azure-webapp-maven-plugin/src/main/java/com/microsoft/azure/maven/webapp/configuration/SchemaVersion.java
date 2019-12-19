/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public enum SchemaVersion {
    V1,
    V2;

    public static final String UNKNOWN_SCHEMA_VERSION = "Unknown value of <schemaVersion> in pom.xml.";
    public static final String V1_SCHEMA_DEPRECATED = "Please use 'v2' in <schemaVersion> in pom.xml.";

    public static SchemaVersion fromString(final String input) throws AzureExecutionException {
        final String schemaVersion = StringUtils.isEmpty(input) ? "v1" : input;

        switch(schemaVersion.toLowerCase(Locale.ENGLISH)) {
            case "v1":
                return V1;
            case "v2":
                return V2;
            default:
                throw new AzureExecutionException(UNKNOWN_SCHEMA_VERSION);
        }
    }
}
