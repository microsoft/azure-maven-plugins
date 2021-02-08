/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.microsoft.azure.toolkit.lib.auth.Account;

import java.util.List;

public interface IPersistenceCache {
    void saveAccount(Account account);

    Account loadAccount();

    void saveSubscriptions(List<SubscriptionEntity> subscriptionList);

    List<SubscriptionEntity> loadSubscriptions();

    void saveEnvironment(String environment);

    String loadEnvironment();

}
