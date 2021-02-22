/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedTokenCredential implements TokenCredential {
    private final TokenCredential tokenCredential;
    private final Map<String, AccessToken> accessTokenCache = new ConcurrentHashMap<>();

    public CachedTokenCredential(TokenCredential tokenCredential) {
        this.tokenCredential = tokenCredential;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        String resource = ScopeUtil.scopesToResource(request.getScopes());
        if (!accessTokenCache.containsKey(resource) || accessTokenCache.get(resource).isExpired()) {
            accessTokenCache.put(resource,
                    this.tokenCredential.getToken(request).block());
        }
        return Mono.just(accessTokenCache.get(resource));
    }
}
