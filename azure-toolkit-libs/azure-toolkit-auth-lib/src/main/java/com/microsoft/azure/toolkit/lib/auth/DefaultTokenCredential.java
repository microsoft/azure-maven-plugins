/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class DefaultTokenCredential extends BaseTokenCredential {
    private TokenCredential onBehalfOf;

    public DefaultTokenCredential(AzureEnvironment environment, TokenCredential onBehalfOf) {
        super(environment);
        Objects.requireNonNull(onBehalfOf, "Cannot create a DefaultTokenCredential from a null TokenCredential.");
        this.onBehalfOf = onBehalfOf;
    }

    @Override
    protected Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext request) {
        return onBehalfOf.getToken(request);
    }
}
