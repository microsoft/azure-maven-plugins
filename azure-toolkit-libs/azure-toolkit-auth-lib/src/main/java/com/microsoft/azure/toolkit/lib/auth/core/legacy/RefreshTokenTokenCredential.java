/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.legacy;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.microsoft.azure.AzureEnvironment;
import reactor.core.publisher.Mono;

public class RefreshTokenTokenCredential implements TokenCredential {
    private Mono<TokenCredential> accessTokenMono;
    private AzureEnvironment environment;
    private String refreshToken;
    private String clientId;
    private String tenantId;

    public RefreshTokenTokenCredential(AzureEnvironment environment, String clientId, String tenantId, String refreshToken) {
        this.environment = environment;
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.refreshToken = refreshToken;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return getAccessToken(tenantId, request);
    }

    protected Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext context) {
        if (accessTokenMono == null) {
            accessTokenMono = Mono.fromCallable(() ->
                    LegacyRefreshTokenCredentialFactory.fromRefreshToken(environment, clientId, tenantId, refreshToken));
        }
        return accessTokenMono.flatMap(accessToken -> accessToken.getToken(context));
    }
}
