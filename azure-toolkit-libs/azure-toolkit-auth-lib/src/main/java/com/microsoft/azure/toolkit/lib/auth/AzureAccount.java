/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;

import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;

public class AzureAccount implements AzureService {
    private Account account;

    @Setter
    @Getter
    private AzureEnvironment environment;

    public List<AccountEntity> getAvailableAccounts() {
        return null;
    }

    public void login(AuthConfiguration auth) throws LoginFailureException {
    }

    public void login(AccountEntity accountEntity) throws AzureToolkitAuthenticationException {
    }

    /**
     * @return the current account
     * @throws AzureToolkitAuthenticationException if not initialized
     */
    public Account account() throws AzureToolkitAuthenticationException {
        return Optional.ofNullable(this.account)
            .orElseThrow(() -> new AzureToolkitAuthenticationException("Account is not initialized."));
    }
}
