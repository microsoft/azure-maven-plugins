/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;


import com.azure.core.util.Configuration;
import com.microsoft.azure.AzureEnvironment;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AuthHelper {
    private static final String AZURE_CHINA = "china";
    private static final String AZURE_GERMANY = "germany";
    private static final String AZURE_GERMAN = "german";
    private static final String AZURE_US_GOVERNMENT = "usgovernment";
    private static final String AZURE_US_GOVERNMENT2 = "us_government";
    private static final String AZURE = "azure";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Map<AzureEnvironment, String> AZURE_ENVIRONMENT_DISPLAY_NAME_MAP = new HashMap<>();

    static {
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE, "AZURE");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_CHINA, "AZURE_CHINA");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_GERMANY, "AZURE_GERMANY");
        AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.put(AzureEnvironment.AZURE_US_GOVERNMENT, "AZURE_US_GOVERNMENT");
    }

    public static String getAzureCloudDisplayName(AzureEnvironment azureEnvironment) {
        return AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.containsKey(azureEnvironment) ?
                AZURE_ENVIRONMENT_DISPLAY_NAME_MAP.get(azureEnvironment) : UNKNOWN;
    }

    public static String getAzureCloudName(AzureEnvironment azureEnvironment) {
        if (azureEnvironment == null) {
            return null;
        }
        // cannot switch since AzureEnvironment is not enum
        if (AZURE.equals(azureEnvironment)) {
            return "AzureCloud";
        }
        if (AZURE_CHINA.equals(azureEnvironment)) {
            return "AzureChinaCloud";
        }
        if (AZURE_GERMANY.equals(azureEnvironment)) {
            return "AzureGermanCloud";
        }
        if (AZURE_US_GOVERNMENT.equals(azureEnvironment)) {
            return "AzureUSGovernment";
        }
        throw new IllegalArgumentException("Unknown azure environment.");
    }

    public static void setupAzureEnvironment(AzureEnvironment env) {
        if (env != null && env != AzureEnvironment.AZURE) {
            // change the default azure env after it is initialized in azure identity
            // see code at
            // https://github.com/Azure/azure-sdk-for-java/blob/32f8f7ca8b44035b2e5520c5e10455f42500a778/sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClientOptions.java#L42
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_AUTHORITY_HOST, env.activeDirectoryEndpoint());
        }
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
        switch (preprocessAzureEnvironment(environment)) {
            case AZURE_CHINA:
            case AZURE_GERMANY:
            case AZURE_GERMAN:
            case AZURE_US_GOVERNMENT:
            case AZURE_US_GOVERNMENT2:
            case AZURE:
                return true;
            default:
                return false;
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

        switch (preprocessAzureEnvironment(environment)) {
            case AZURE_CHINA:
                return AzureEnvironment.AZURE_CHINA;
            case AZURE_GERMANY:
            case AZURE_GERMAN: // the TYPO comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
                return AzureEnvironment.AZURE_GERMANY;
            case AZURE_US_GOVERNMENT:
            case AZURE_US_GOVERNMENT2:
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }

    private static String preprocessAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return environment;
        }
        String formattedEnvironment = StringUtils.trim(StringUtils.lowerCase(environment));
        if (StringUtils.equals(formattedEnvironment, AZURE)) {
            return AZURE;
        }
        for (String startToRemove : Arrays.asList("azure-", "azure_", "azure")) {
            if (StringUtils.startsWith(formattedEnvironment, startToRemove)) {
                formattedEnvironment = StringUtils.removeStart(formattedEnvironment, startToRemove);
                break;
            }
        }
        for (String endToRemove : Arrays.asList("-cloud", "_cloud", "cloud")) {
            if (StringUtils.endsWith(formattedEnvironment, endToRemove)) {
                formattedEnvironment = StringUtils.removeEnd(formattedEnvironment, endToRemove);
                break;
            }
        }

        return formattedEnvironment;
    }
}
