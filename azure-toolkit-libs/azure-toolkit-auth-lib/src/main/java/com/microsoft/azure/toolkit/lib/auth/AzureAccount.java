/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AccountProfile;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AzureAccount implements AzureService {

    private Account account;

    @Setter
    @Getter
    private AzureEnvironment environment;

    public List<Account> getAvailableAccounts() {
        return null;
    }

    /**
     * @return account if authenticated
     * @throws AzureToolkitAuthenticationException if failed to authenticate
     */
    public Account login(AccountProfile profile) throws AzureToolkitAuthenticationException {
        Objects.requireNonNull(profile, "profile is required.");
        this.account = new Account();
        final AccountEntity entity = new AccountEntity();
        entity.setMethod(profile.getMethod());
        entity.setAuthenticated(profile.isAuthenticated());
        entity.setEnvironment(profile.getEnvironment());
        entity.setEmail(profile.getEmail());
        entity.setTenantIds(profile.getTenantIds());
        this.account.setEntity(entity);
        this.account.setCredentialBuilder(profile.getCredentialBuilder());
        if (!profile.isAuthenticated()) {
            return this.account;
        }
        // get
        this.account.initialize();
        this.account.selectSubscriptions(profile.getSelectedSubscriptionIds());
        return this.account;
    }

    /**
     * @return account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }
}
