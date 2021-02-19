/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ClientAuthenticationException;
import com.google.common.base.MoreObjects;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import lombok.Setter;
import rx.Single;

public abstract class AbstractCredentialRetriever implements ICredentialRetriever {

    @Setter
    protected AzureEnvironment env;

    public AbstractCredentialRetriever(AzureEnvironment env) {
        this.env = env;
    }

    @Override
    public Single<AzureCredentialWrapper> retrieve() {
        return Single.fromCallable(this::retrieveInternal);
    }

    public abstract AzureCredentialWrapper retrieveInternal() throws LoginFailureException;

    protected void validateTokenCredential(TokenCredential credential) throws LoginFailureException {
        try {
            credential.getToken(
                new TokenRequestContext().addScopes(getAzureEnvironment().managementEndpoint() + "/.default")).block().getToken();
        } catch (ClientAuthenticationException ex) {
            throw new LoginFailureException(ex.getMessage(), ex);
        }
    }

    protected AzureEnvironment getAzureEnvironment() {
        return MoreObjects.firstNonNull(env, AzureEnvironment.AZURE);
    }
}
