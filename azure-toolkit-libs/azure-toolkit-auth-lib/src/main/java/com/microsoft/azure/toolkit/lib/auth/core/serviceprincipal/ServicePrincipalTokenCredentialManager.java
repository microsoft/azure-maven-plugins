/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.TokenCredentialManagerWithCache;

import javax.annotation.Nonnull;

class ServicePrincipalTokenCredentialManager extends TokenCredentialManagerWithCache {

    public ServicePrincipalTokenCredentialManager(@Nonnull AzureEnvironment env, @Nonnull TokenCredential credential) {
        this.env = env;
        this.rootCredentialSupplier = () -> credential;
        this.credentialSupplier = tenant -> credential;
    }
}
