/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.AzureTokenCredentialsAdapter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class AzureTokenCredentialsConverter {
    public static AzureTokenCredentials convert(AzureEnvironment env, String tenantId, TokenCredential tokenCredential) {
        return new AzureTokenCredentialsAdapter(toV1Environment(env), tenantId, tokenCredential);
    }

    private static com.microsoft.azure.AzureEnvironment toV1Environment(AzureEnvironment env) {
        return Arrays.stream(com.microsoft.azure.AzureEnvironment.knownEnvironments())
                .filter(e -> StringUtils.equalsIgnoreCase(env.getManagementEndpoint(), e.managementEndpoint()))
                .findFirst().orElse(com.microsoft.azure.AzureEnvironment.AZURE);
    }
}
