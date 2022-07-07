/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.managedidentity;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import lombok.Getter;

import javax.annotation.Nonnull;

public class ManagedIdentityAccount extends Account {
    @Getter
    private final AuthType type = AuthType.MANAGED_IDENTITY;

    public ManagedIdentityAccount() {
        super(new AuthConfiguration(AuthType.MANAGED_IDENTITY));
    }

    public ManagedIdentityAccount(@Nonnull final AuthConfiguration config) {
        super(config);
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        return new ManagedIdentityCredentialBuilder().clientId(this.getClientId()).build();
    }

    @Override
    public boolean checkAvailable() {
        return this.getManagementToken().isPresent();
    }
}
