/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;

public class VisualStudioCodeCredentialRetriever extends AbstractCredentialRetriever {

    public VisualStudioCodeCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        throw new UnsupportedOperationException("Fix me");
    }
}
