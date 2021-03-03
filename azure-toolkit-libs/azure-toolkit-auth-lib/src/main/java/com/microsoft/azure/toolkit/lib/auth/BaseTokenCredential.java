/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public abstract class BaseTokenCredential implements TokenCredential {
    @Setter
    @Getter
    private AzureEnvironment environment;

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return getAccessToken(null, request);
    }

    protected abstract Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext request);

    public TokenCredential createTenantTokenCredential(String tenantId) {
        return new CachedTokenCredential(request -> getAccessToken(tenantId, request));
    }
}
