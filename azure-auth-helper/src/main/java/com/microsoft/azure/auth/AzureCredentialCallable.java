/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureCredentialCallable implements Callable<AzureCredential> {
    private String baseUrl;
    private AcquireTokenFunction acquireTokenFunc;

    public AzureCredentialCallable(String baseUrl, AcquireTokenFunction acquireTokenFunc) {
        if (baseUrl == null) {
            throw new NullPointerException("Parameter 'baseUrl' cannot be null.");
        }

        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Parameter 'baseUrl' cannot be empty.");
        }

        if (acquireTokenFunc == null) {
            throw new NullPointerException("Parameter 'acquireTokenFunc' cannot be null.");
        }

        this.baseUrl = baseUrl;
        this.acquireTokenFunc = acquireTokenFunc;
    }

    @Override
    public AzureCredential call() throws AzureLoginFailureException {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final AuthenticationContext authenticationContext = new AuthenticationContext(baseUrl, true,
                    executorService);
            return AzureCredential.fromAuthenticationResult(this.acquireTokenFunc.acquire(authenticationContext));
        } catch (Throwable e) {
            throw new AzureLoginFailureException(e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }
}
