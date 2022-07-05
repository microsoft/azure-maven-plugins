/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.auth;


import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convert token credential in azure-identity to legacy AzureTokenCredentials
 * Refers https://github.com/jongio/azidext/blob/master/java/src/main/java/com/azure/identity/extensions/AzureIdentityCredentialAdapter.java
 */
class AzureTokenCredentialsAdapter extends AzureTokenCredentials {
    private final TokenCredential tokenCredential;
    private final Map<String, AccessToken> accessTokenCache = new ConcurrentHashMap<>();

    AzureTokenCredentialsAdapter(AzureEnvironment environment, String tenantId,
                                 TokenCredential tokenCredential) {
        super(environment, tenantId);
        this.tokenCredential = tokenCredential;
    }

    @Override
    public String getToken(String endpoint) {
        if (!accessTokenCache.containsKey(endpoint) || accessTokenCache.get(endpoint).isExpired()) {
            accessTokenCache.put(endpoint,
                this.tokenCredential.getToken(new TokenRequestContext().addScopes(ScopeUtil.resourceToScopes(endpoint))).block());
        }
        return accessTokenCache.get(endpoint).getToken();
    }

    public static AzureTokenCredentials from(com.azure.core.management.AzureEnvironment env, String tenantId, TokenCredential tokenCredential) {
        AzureEnvironment azureEnvironment = Arrays.stream(AzureEnvironment.knownEnvironments())
            .filter(e -> StringUtils.equalsIgnoreCase(env.getManagementEndpoint(), e.managementEndpoint()))
            .findFirst().orElse(AzureEnvironment.AZURE);

        return new AzureTokenCredentialsAdapter(azureEnvironment, tenantId, tokenCredential);
    }

}
