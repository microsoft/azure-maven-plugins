/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.SimpleTokenCache;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenCredentialManagerWithCache extends TokenCredentialManager {
    // cache for different tenants
    private final Map<String, TokenCredential> tokenCredentialCache = new ConcurrentHashMap<>();

    public TokenCredential createTokenCredentialForTenant(String tenantId) {
        return this.tokenCredentialCache.computeIfAbsent(tenantId,
                key -> new CachedTokenCredential(super.createTokenCredentialForTenant(tenantId)));
    }

    static class CachedTokenCredential implements TokenCredential {
        // cache for different resources on the same tenant
        private final Map<String, SimpleTokenCache> tokenCache = new ConcurrentHashMap<>();

        private final TokenCredential tokenCredential;

        public CachedTokenCredential(TokenCredential tokenCredential) {
            this.tokenCredential = tokenCredential;
        }

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            String resource = ScopeUtil.scopesToResource(request.getScopes());
            return tokenCache.computeIfAbsent(resource, (ignore) ->
                    new SimpleTokenCache(() -> tokenCredential.getToken(request))).getToken();
        }
    }
}
