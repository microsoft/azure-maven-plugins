/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.microsoft.azure.tools.auth.exception.AzureLoginException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

public class ChainedCredentialRetriever implements ICredentialRetriever {
    private List<Single<AzureCredentialWrapper>> retrieveFunctions = new ArrayList<>();

    public void addRetriever(ICredentialRetriever credentialRetriever) {
        this.retrieveFunctions.add(credentialRetriever.retrieve());
    }

    public Single<AzureCredentialWrapper> retrieve() {
        if (retrieveFunctions.isEmpty()) {
            return Single.error(new AzureLoginException("No retrievers are defined to get azure credentials."));
        }
        Single<AzureCredentialWrapper> function = retrieveFunctions.get(0);
        for (int i = 1; i < retrieveFunctions.size(); i++) {
            function = function.onErrorResumeNext(retrieveFunctions.get(i));
        }
        return function;
    }
}
