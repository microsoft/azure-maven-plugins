/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.exception;

import com.google.common.base.Throwables;
import com.microsoft.aad.adal4j.AuthenticationException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Interceptor to handle REST API related exceptions
 */
public class RestExceptionHandlerInterceptor implements Interceptor {
    @Override
    public Response intercept(final Chain chain) throws IOException {
        try {
            final Request request = chain.request();
            return chain.proceed(request);
        } catch (final Exception ex) {
            final List<Throwable> exceptions = Throwables.getCausalChain(ex);
            if (exceptions.stream().anyMatch(e -> e instanceof UnknownHostException)) {
                throw new AzureToolkitRuntimeException("Unknown host! You network condition maybe unstable, please try later.");
            } else if (exceptions.stream().anyMatch(e -> e instanceof AuthenticationException)) {
                throw new AzureToolkitRuntimeException("Invalid authentication! You may sign in again or run \"az login\" if using Azure CLI credential");
            }
            throw ex;
        }
    }
}
