/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.legacy;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import reactor.core.publisher.Mono;

public class LegacyAsyncCredentialProxy implements TokenCredential {
    private Mono<TokenCredential> credentialMono;

    private TokenCredential credential;

    public LegacyAsyncCredentialProxy(Mono<TokenCredential> credentialMono) {
        this.credentialMono = credentialMono;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        if (credential == null) {
            credential = credentialMono.block();
        }
        return credential.getToken(request);
    }
}
