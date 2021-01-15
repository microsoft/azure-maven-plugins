/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.CredentialUnavailableException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import lombok.Setter;
import rx.Single;

public abstract class AbstractCredentialRetriever implements ICredentialRetriever {

    @Setter
    protected AzureEnvironment env;

    @Override
    public Single<AzureCredentialWrapper> retrieve() {
        return Single.fromCallable(this::retrieveInternal);
    }

    public abstract AzureCredentialWrapper retrieveInternal() throws LoginFailureException;

    protected void validateTokenCredential(TokenCredential credential) throws LoginFailureException {
        try {
            credential.getToken(
                    new TokenRequestContext().addScopes((env == null ? AzureEnvironment.AZURE : env).managementEndpoint() + ".default")).block().getToken();
        } catch (CredentialUnavailableException ex) {
            throw new LoginFailureException(ex.getMessage(), ex);
        }
    }
}
