/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;


import com.azure.core.util.Configuration;
import com.microsoft.azure.AzureEnvironment;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthHelper {
    private static final String UNKNOWN = "UNKNOWN";

    private static final Map<AzureEnvironment, String> AZURE_CLOUD_DISPLAY_NAME_MAP = new HashMap<>();
    private static final Map<String, AzureEnvironment> AZURE_CLOUD_ALIAS_MAP = new CaseInsensitiveMap<>();

    static {
        putAliasMap(AzureEnvironment.AZURE, "AzureCloud", "azure");
        putAliasMap(AzureEnvironment.AZURE_CHINA, "AzureChinaCloud", "azure_china");
        // the TYPO:azure_german comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
        putAliasMap(AzureEnvironment.AZURE_GERMANY, "AzureGermanCloud", "azure_germany", "azure_german");
        putAliasMap(AzureEnvironment.AZURE_US_GOVERNMENT, "AzureUSGovernment", "azure_us_government");
    }

    public static String getAzureCloudDisplayName(AzureEnvironment azureEnvironment) {
        return AZURE_CLOUD_DISPLAY_NAME_MAP.containsKey(azureEnvironment) ?
                AZURE_CLOUD_DISPLAY_NAME_MAP.get(azureEnvironment) : UNKNOWN;
    }

    public static String getAzureCloudName(AzureEnvironment azureEnvironment) {
        if (azureEnvironment == null) {
            return null;
        }
        // cannot switch since AzureEnvironment is not enum
        if (AzureEnvironment.AZURE.equals(azureEnvironment)) {
            return "AzureCloud";
        }
        if (AzureEnvironment.AZURE_CHINA.equals(azureEnvironment)) {
            return "AzureChinaCloud";
        }
        if (AzureEnvironment.AZURE_GERMANY.equals(azureEnvironment)) {
            return "AzureGermanCloud";
        }
        if (AzureEnvironment.AZURE_US_GOVERNMENT.equals(azureEnvironment)) {
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
        return AZURE_CLOUD_ALIAS_MAP.containsKey(environment);
    }

    /**
     * Parse the corresponding azure environment.
     *
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    public static AzureEnvironment parseAzureEnvironment(String environment) {
        if (AZURE_CLOUD_ALIAS_MAP.containsKey(environment)) {
            return AZURE_CLOUD_ALIAS_MAP.get(environment);
        }
        return null;
    }

    private static void putAliasMap(AzureEnvironment env, String... aliases) {
        if (Objects.isNull(aliases) || aliases.length < 2) {
            throw new IllegalArgumentException("Expect at least two aliases for azure environment.");
        }
        boolean endsWithCloud = StringUtils.endsWithIgnoreCase(aliases[0], "cloud");
        AZURE_CLOUD_ALIAS_MAP.put(aliases[0], env);
        AZURE_CLOUD_DISPLAY_NAME_MAP.put(env, aliases[1]);
        for (String alias : ArrayUtils.subarray(aliases, 1, aliases.length)) {
            AZURE_CLOUD_ALIAS_MAP.put(alias, env);
            if (endsWithCloud) {
                AZURE_CLOUD_ALIAS_MAP.put(alias + "_cloud", env);
                AZURE_CLOUD_ALIAS_MAP.put(StringUtils.replace(alias + "_cloud", "-", "_"), env);
            }
        }
    }

    public static void main(String[] args) {
        for (String key : AZURE_CLOUD_ALIAS_MAP.keySet()) {
            System.out.println(key + " = " + getAzureCloudName(AZURE_CLOUD_ALIAS_MAP.get(key))
                    + "\n\tAzureCli : " + getAzureCloudName(parseAzureEnvironment(key)) + "\n\tDisplay Name : "
                    + getAzureCloudDisplayName(parseAzureEnvironment(key)));
        }
    }
}
