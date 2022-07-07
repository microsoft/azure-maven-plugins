/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.Configuration;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AzureEnvironmentUtils {
    private static final Map<AzureEnvironment, String[]> AZURE_CLOUD_ALIAS_MAP = new HashMap<>();
    private static final String CHINA_PORTAL = "https://portal.azure.cn";
    private static final String GLOBAL_PORTAL = "https://ms.portal.azure.com";

    static {
        // the first alias is the cloud name in azure cli
        // the second alias is the display name, all other aliases are only used in our toolkit
        putAliasMap(AzureEnvironment.AZURE, "AzureCloud", "azure", "azure_cloud", "GLOBAL");
        putAliasMap(AzureEnvironment.AZURE_CHINA, "AzureChinaCloud", "azure_china", "AzureChina", "azure_china_cloud", "CHINA");
        // the TYPO:azure_german comes from azure cli: https://docs.microsoft.com/en-us/azure/germany/germany-get-started-connect-with-cli
        putAliasMap(AzureEnvironment.AZURE_GERMANY, "AzureGermanCloud", "azure_germany", "azure_german",
            "azure_germany_cloud", "azure_german_cloud", "AzureGerman", "AzureGermany", "GERMAN");
        putAliasMap(AzureEnvironment.AZURE_US_GOVERNMENT, "AzureUSGovernment", "azure_us_government", "US_GOVERNMENT");
    }

    public static String azureEnvironmentToString(AzureEnvironment azureEnvironment) {
        if (AZURE_CLOUD_ALIAS_MAP.containsKey(azureEnvironment)) {
            return AZURE_CLOUD_ALIAS_MAP.get(azureEnvironment)[1];
        }
        throw new IllegalArgumentException("Unknown azure environment.");
    }

    public static String getCloudName(AzureEnvironment azureEnvironment) {
        if (AZURE_CLOUD_ALIAS_MAP.containsKey(azureEnvironment)) {
            return AZURE_CLOUD_ALIAS_MAP.get(azureEnvironment)[0];
        }
        throw new IllegalArgumentException("Unknown azure environment.");
    }

    public static String getAuthority(AzureEnvironment environment) {
        return environment.getActiveDirectoryEndpoint().replaceAll("/+$", "");
    }

    /**
     * Parse the corresponding azure environment.
     *
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    @Nullable
    public static AzureEnvironment stringToAzureEnvironment(String environment) {
        final String targetEnvironment = StringUtils.replaceChars(environment, '-', '_');
        return AZURE_CLOUD_ALIAS_MAP.entrySet().stream().filter(entry -> Utils.containsIgnoreCase(Arrays.asList(entry.getValue()), targetEnvironment))
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    public static String getPortalUrl(AzureEnvironment env) {
        if (AzureEnvironment.AZURE.equals(env)) {
            return GLOBAL_PORTAL;
        } else if (AzureEnvironment.AZURE_CHINA.equals(env)) {
            return CHINA_PORTAL;
        } else if (AzureEnvironment.AZURE_GERMANY.equals(env)) {
            return AzureEnvironment.AZURE_GERMANY.getPortal();
        } else if (AzureEnvironment.AZURE_US_GOVERNMENT.equals(env)) {
            return AzureEnvironment.AZURE_US_GOVERNMENT.getPortal();
        } else {
            return env.getPortal();
        }
    }

    private static void putAliasMap(AzureEnvironment env, String... aliases) {
        AZURE_CLOUD_ALIAS_MAP.put(env, aliases);
    }
}
