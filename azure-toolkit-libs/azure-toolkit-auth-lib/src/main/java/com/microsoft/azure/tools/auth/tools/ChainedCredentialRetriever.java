/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.tools;

import com.microsoft.azure.tools.auth.exception.AzureLoginException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.functions.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChainedCredentialRetriever {
    public interface RetrieveCredentialFunction<T> extends Function {
        T call() throws AzureLoginException;
    }
    private List<Func1<Throwable, Single<AzureCredentialWrapper>>> retrieveFunctions = new ArrayList<>();

    public void addRetrieveFunction(RetrieveCredentialFunction<Single<AzureCredentialWrapper>> func1) {
        this.retrieveFunctions.add((e) -> {
            try {
                return Objects.requireNonNull(func1.call());
            } catch (AzureLoginException ex) {
                throw Exceptions.propagate(e);
            }
        });

    }

    public Single<AzureCredentialWrapper> retrieve() {
        if (retrieveFunctions.isEmpty()) {
            return Single.error(new AzureLoginException("No retrievers are defined to get azure credentials."));
        }
        Single<AzureCredentialWrapper> function = retrieveFunctions.get(0).call(null);
        for (int i = 1; i < retrieveFunctions.size(); i++) {
            function = function.onErrorResumeNext(retrieveFunctions.get(i));
        }
        return function;
    }
}
