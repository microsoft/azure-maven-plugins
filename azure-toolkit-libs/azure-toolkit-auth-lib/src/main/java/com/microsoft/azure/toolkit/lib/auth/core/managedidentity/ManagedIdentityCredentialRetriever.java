/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.managedidentity;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;

import javax.annotation.Nonnull;

public class ManagedIdentityCredentialRetriever extends AbstractCredentialRetriever {

    private String clientId;

    public ManagedIdentityCredentialRetriever(@Nonnull AuthConfiguration configuration) {
        super(configuration.getEnvironment());
        // it allows null when it is a System Managed Identity
        clientId = configuration.getClient();
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        ManagedIdentityCredential managedIdentityCredential = new ManagedIdentityCredentialBuilder().clientId(clientId).build();
        validateTokenCredential(managedIdentityCredential);
        return new AzureCredentialWrapper(AuthMethod.MANAGED_IDENTITY, managedIdentityCredential, getAzureEnvironment());
    }
}
