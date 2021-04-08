/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.refresktoken;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.TenantCredential;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RefreshTokenTokenCredential extends TenantCredential {
    private final Map<String, TokenCredential> accessTokenCache = new ConcurrentHashMap<>();
    private String refreshToken;
    private String clientId;

    public RefreshTokenTokenCredential(String clientId, String refreshToken) {
        this.clientId = clientId;
        this.refreshToken = refreshToken;
    }

    @Override
    protected Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext context) {
        String key = StringUtils.firstNonBlank(tenantId, "$");
        if (!accessTokenCache.containsKey(key)) {
            accessTokenCache.put(key,
                    // TODO: hard coded env, will be fixed in future PR
                    RefreshTokenCredentialFactory.fromRefreshToken(AzureEnvironment.AZURE, clientId, tenantId, refreshToken));
        }
        return accessTokenCache.get(key).getToken(context);
    }
}
