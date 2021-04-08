/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCliSubscription;
import com.microsoft.azure.toolkit.lib.auth.util.AzureCliUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import java.util.List;
import java.util.stream.Collectors;

public class AzureCliAccount extends Account {
    @Override
    protected boolean checkAvailableInner() {
        try {
            if (!AzureCliUtils.checkCliVersion()) {
                return false;
            }
            AzureCliUtils.executeAzCommandJson("az account get-access-token --output json");
            return true;
        } catch (Throwable ex) {
            this.entity.setLastError(ex);
            return false;
        }
    }

    @Override
    protected void initializeCredentials() throws LoginFailureException {
        List<AzureCliSubscription> subscriptions = AzureCliUtils.listSubscriptions();
        if (subscriptions.isEmpty()) {
            throw new LoginFailureException("Cannot find any subscriptions in current account.");
        }

        AzureCliSubscription defaultSubscription = subscriptions.stream()
                .filter(AzureCliSubscription::isSelected).findFirst().orElse(subscriptions.get(0));

        this.entity.setEnvironment(defaultSubscription.getEnvironment());

        this.entity.setEmail(defaultSubscription.getEmail());

        AzureCliTokenCredential azureCliCredential = new AzureCliTokenCredential();

        verifyTokenCredential(defaultSubscription.getEnvironment(), azureCliCredential);

        // use the tenant who has one or more subscriptions
        this.entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));

        // set initial selection of subscriptions
        this.entity.setSelectedSubscriptionIds(subscriptions.stream().filter(Subscription::isSelected)
                .map(Subscription::getId).distinct().collect(Collectors.toList()));

        this.entity.setTenantCredential(azureCliCredential);
    }

    @Override
    public AuthMethod getMethod() {
        return AuthMethod.AZURE_CLI;
    }
}
