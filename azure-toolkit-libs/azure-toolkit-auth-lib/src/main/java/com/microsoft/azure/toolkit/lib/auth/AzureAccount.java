/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.model.AccountEntity;
import com.microsoft.azure.toolkit.lib.auth.model.IPersistenceCache;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class AzureAccount {

    @Setter
    @Getter
    private AzureEnvironment environment;

    private List<SubscriptionEntity> selectedSubscriptions;

    private List<SubscriptionEntity> subscriptions;

    public List<Account> getAvailableAccounts() {
        return null;
    }

    public Account login(AccountEntity account) {
        return null;
    }

    public Account account() {
        return null;
    }

    public void withPersistenceCache(IPersistenceCache cache) {

    }
}
