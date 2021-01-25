/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;


import com.azure.core.util.Configuration;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.common.util.StringListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AuthHelper {
    private static final Map<AzureEnvironment, String[]> AZURE_CLOUD_ALIAS_MAP = new HashMap<>();

    static {
        // the first alias is the cloud name in azure cli
        // the second alias is the display name, all other aliases are only used in our toolkit
        putAliasMap(AzureEnvironment.AZURE, "AzureCloud", "azure", "azure_cloud");
        putAliasMap(AzureEnvironment.AZURE_CHINA, "AzureChinaCloud", "azure_china", "AzureChina", "azure_china_cloud");
        // the TYPO:azure_german comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
        putAliasMap(AzureEnvironment.AZURE_GERMANY, "AzureGermanCloud", "azure_germany", "azure_german",
                "azure_germany_cloud", "azure_german_cloud", "AzureGerman", "AzureGermany");
        putAliasMap(AzureEnvironment.AZURE_US_GOVERNMENT, "AzureUSGovernment", "azure_us_government");
    }

    public static String azureEnvironmentToString(AzureEnvironment azureEnvironment) {
        if (AZURE_CLOUD_ALIAS_MAP.containsKey(azureEnvironment)) {
            return AZURE_CLOUD_ALIAS_MAP.get(azureEnvironment)[1];
        }
        throw new IllegalArgumentException("Unknown azure environment.");
    }

    public static String getCloudNameForAzureCli(AzureEnvironment azureEnvironment) {
        if (AZURE_CLOUD_ALIAS_MAP.containsKey(azureEnvironment)) {
            return AZURE_CLOUD_ALIAS_MAP.get(azureEnvironment)[0];
        }
        throw new IllegalArgumentException("Unknown azure environment.");
    }

    public static void setupAzureEnvironment(AzureEnvironment env) {
        setPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity", "off");
        setPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4jextensions", "off");
        if (env != null && env != AzureEnvironment.AZURE) {
            // change the default azure env after it is initialized in azure identity
            // see code at
            // https://github.com/Azure/azure-sdk-for-java/blob/32f8f7ca8b44035b2e5520c5e10455f42500a778/sdk/identity/azure-identity/
            // src/main/java/com/azure/identity/implementation/IdentityClientOptions.java#L42
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_AUTHORITY_HOST, env.activeDirectoryEndpoint());
        }
    }

    /**
     * Parse the corresponding azure environment.
     *
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    public static AzureEnvironment stringToAzureEnvironment(String environment) {
        final String targetEnvironment = StringUtils.replaceChars(environment, '-', '_');
        return AZURE_CLOUD_ALIAS_MAP.entrySet().stream().filter(entry -> StringListUtils.containsIgnoreCase(Arrays.asList(entry.getValue()), targetEnvironment))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private static void putAliasMap(AzureEnvironment env, String... aliases) {
        AZURE_CLOUD_ALIAS_MAP.put(env, aliases);
    }

    private static void setPropertyIfNotExist(String key, String value) {
        if (StringUtils.isBlank(System.getProperty(key))) {
            System.setProperty(key, value);
        }
    }
}
