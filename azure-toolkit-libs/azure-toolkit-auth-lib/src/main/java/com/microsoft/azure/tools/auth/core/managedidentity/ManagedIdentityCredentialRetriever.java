/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core.managedidentity;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;

public class ManagedIdentityCredentialRetriever extends AbstractCredentialRetriever {

    public ManagedIdentityCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder().build();
        validateTokenCredential(managedIdentityCredential);
        return new AzureCredentialWrapper(AuthMethod.MANAGED_IDENTITY, managedIdentityCredential, getAzureEnvironment());
    }
}
