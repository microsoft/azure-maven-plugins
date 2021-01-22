/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.tools.auth;


import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convert token credential in azure-identity to legacy AzureTokenCredentials
 * Refers https://github.com/jongio/azidext/blob/master/java/src/main/java/com/azure/identity/extensions/AzureIdentityCredentialAdapter.java
 */
public class AzureIdentityCredentialTokenCredentials extends AzureTokenCredentials {
    private final TokenCredential tokenCredential;
    private final Map<String, AccessToken> accessTokenCache = new ConcurrentHashMap<>();
    private final String[] scopes;

    public AzureIdentityCredentialTokenCredentials(String tenantId, TokenCredential tokenCredential) {
        this(AzureEnvironment.AZURE, tenantId, tokenCredential, new String[]{ AzureEnvironment.AZURE.managementEndpoint() });
    }

    public AzureIdentityCredentialTokenCredentials(AzureEnvironment environment, String tenantId, TokenCredential tokenCredential) {
        this(environment, tenantId, tokenCredential, new String[]{ environment.managementEndpoint() + "/.default" });
    }

    public AzureIdentityCredentialTokenCredentials(AzureEnvironment environment, String tenantId,
                                                   TokenCredential tokenCredential, String[] scopes) {
        super(environment, tenantId);
        this.tokenCredential = tokenCredential;
        this.scopes = scopes;
    }

    @Override
    public String getToken(String endpoint) {
        if (!accessTokenCache.containsKey(endpoint) || accessTokenCache.get(endpoint).isExpired()) {
            accessTokenCache.put(endpoint,
                    this.tokenCredential.getToken(new TokenRequestContext().addScopes(scopes)).block());
        }
        return accessTokenCache.get(endpoint).getToken();
    }
}
