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
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@Slf4j
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
        final boolean available = this.getManagementToken().isPresent();
        log.trace("Auth type ({}) is {}available.", TextUtils.cyan(this.getType().name()), available ? "" : TextUtils.yellow("NOT "));
        return available;
    }
}
