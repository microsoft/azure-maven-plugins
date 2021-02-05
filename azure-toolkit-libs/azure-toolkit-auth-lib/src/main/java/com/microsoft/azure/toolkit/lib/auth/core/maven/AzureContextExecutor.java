/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.maven;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureContextExecutor {
    private String baseUrl;
    private AcquireTokenFunction acquireTokenFunc;

    public AzureContextExecutor(String baseUrl, AcquireTokenFunction acquireTokenFunc) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Parameter 'baseUrl' cannot be empty.");
        }

        if (acquireTokenFunc == null) {
            throw new IllegalArgumentException("Parameter 'acquireTokenFunc' cannot be null.");
        }

        this.baseUrl = baseUrl;
        this.acquireTokenFunc = acquireTokenFunc;
    }

    public AzureCredential execute() throws MalformedURLException, InterruptedException, ExecutionException {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final AuthenticationContext authenticationContext = new AuthenticationContext(baseUrl, true,
                    executorService);
            final AuthenticationResult result = this.acquireTokenFunc.acquire(authenticationContext);
            if (result == null) {
                return null;
            }
            return AzureCredential.fromAuthenticationResult(result);
        } finally {
            executorService.shutdown();
        }
    }
}
