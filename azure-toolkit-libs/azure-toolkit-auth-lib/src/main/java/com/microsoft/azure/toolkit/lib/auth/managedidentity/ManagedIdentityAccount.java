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
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class ManagedIdentityAccount extends Account {
    @Nullable
    private final AuthConfiguration config;

    public ManagedIdentityAccount(@Nullable final AuthConfiguration config) {
        super(AuthType.MANAGED_IDENTITY);
        this.config = config;
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

    public String getClientId() {
        return Optional.ofNullable(this.config).map(AuthConfiguration::getClient).orElse(null);
    }
}
