/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;


import com.azure.core.util.Configuration;
import com.microsoft.azure.AzureEnvironment;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthHelper {
    private static final String AZURE_CHINA = "azurechina";
    private static final String AZURE_CHINA_CLOUD = "azurechinacloud";
    private static final String AZURE_GERMANY = "azuregermany";
    private static final String AZURE_GERMAN_CLOUD = "azuregermancloud";
    private static final String AZURE_US_GOVERNMENT = "azureusgovernment";
    private static final String AZURE = "azure";
    private static final String AZURE_CLOUD = "azurecloud";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Map<AzureEnvironment, String> AZURE_ENVIRONMENT_DISPLAY_NAME_MAP = new HashMap<>();

    static {
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE, "AZURE");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_CHINA, "AZURE_CHINA");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_GERMANY, "AZURE_GERMANY");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_US_GOVERNMENT, "AZURE_US_GOVERNMENT");
    }

    public static String getAzureEnvironmentDisplayName(AzureEnvironment azureEnvironment) {
        return AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.containsKey(azureEnvironment) ?
                AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.get(azureEnvironment) : UNKNOWN;
    }

    public static void setupAzureEnvironment(AzureEnvironment environment) {
        AzureEnvironment env = environment;
        if (env == null) {
            env = AzureEnvironment.AZURE;
        }
        // change the default azure env after it is initialized in azure identity
        // see code at
        // https://github.com/Azure/azure-sdk-for-java/blob/32f8f7ca8b44035b2e5520c5e10455f42500a778/sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClientOptions.java#L42
        Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_AUTHORITY_HOST, env.activeDirectoryEndpoint());
    }

    /**
     * Validate the azure environment.
     *
     * @param environment the environment string
     * @return true if the environment string is a valid azure environment
     */
    public static boolean validateEnvironment(String environment) {
        if (StringUtils.isBlank(environment)) {
            return true;
        }
        switch (environment.toUpperCase(Locale.ENGLISH)) {
            case AZURE_CHINA:
            case AZURE_CHINA_CLOUD:
            case AZURE_GERMANY:
            case AZURE_GERMAN_CLOUD:
            case AZURE_US_GOVERNMENT:
            case AZURE:
            case AZURE_CLOUD:
                return true;
            default : return false;
        }
    }

    /**
     * Parse the corresponding azure environment.
     *
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    public static AzureEnvironment parseAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return null;
        }

        switch (StringUtils.remove(environment.toLowerCase(Locale.ENGLISH), "_")) {
            case AZURE_CHINA:
            case AZURE_CHINA_CLOUD: // this value comes from azure cli
                return AzureEnvironment.AZURE_CHINA;
            case AZURE_GERMANY:
            case AZURE_GERMAN_CLOUD: // the TYPO comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
                return AzureEnvironment.AZURE_GERMANY;
            case AZURE_US_GOVERNMENT:
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }
}
