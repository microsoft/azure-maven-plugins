/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.auth.exception.AzureLoginTimeoutException;

import java.util.concurrent.ExecutionException;

@FunctionalInterface
public interface AcquireTokenFunction {
    AuthenticationResult acquire(AuthenticationContext context) throws AzureLoginTimeoutException, InterruptedException, ExecutionException;
}
