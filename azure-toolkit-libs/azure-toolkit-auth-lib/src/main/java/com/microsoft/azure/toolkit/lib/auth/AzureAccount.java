/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;

import java.util.List;
import java.util.Optional;

public class AzureAccount implements AzureService {

    private Account account;

    public List<Account> getAvailableAccounts() {
        return null;
    }

    /**
     * @return account if authenticated
     * @throws AzureToolkitAuthenticationException if failed to authenticate
     */
    public Account login(AccountEntity entity) throws AzureToolkitAuthenticationException {
        final Account account = new Account(); // TODO: login and create Account instance;
        this.account = account;
        return this.account;
    }

    /**
     * @return account if authenticated
     * @throws AzureToolkitAuthenticationException if not authenticated
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
            .filter(Account::isAuthenticated)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not authentication."));
    }
}
