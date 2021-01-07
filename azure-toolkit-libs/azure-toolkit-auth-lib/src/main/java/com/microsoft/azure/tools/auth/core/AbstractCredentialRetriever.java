/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;

public abstract class AbstractCredentialRetriever implements ICredentialRetriever {
    public Single<AzureCredentialWrapper> retrieve() {
        try {
            return Single.just(retrieveInternal());
        } catch (LoginFailureException ex) {
            return Single.error(ex);
        }
    }

    public abstract AzureCredentialWrapper retrieveInternal() throws LoginFailureException;
}
