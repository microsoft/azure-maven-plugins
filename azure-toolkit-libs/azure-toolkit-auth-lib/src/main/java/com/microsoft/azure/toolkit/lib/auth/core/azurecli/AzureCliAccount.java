/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthMethod;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCliSubscription;
import com.microsoft.azure.toolkit.lib.auth.util.AzureCliUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class AzureCliAccount extends Account {
    @Override
    protected boolean checkAvailableInner() {
        try {
            AzureCliUtils.ensureMinimumCliVersion();
            AzureCliUtils.executeAzCommandJson("az account get-access-token --output json");
            return true;
        } catch (Throwable ex) {
            throw new AzureToolkitAuthenticationException(
                    "Cannot login through azure cli due to error:" + ex.getMessage());
        }
    }

    @Override
    protected TokenCredential createTokenCredential() {
        List<AzureCliSubscription> subscriptions = AzureCliUtils.listSubscriptions();
        if (subscriptions.isEmpty()) {
            throw new AzureToolkitAuthenticationException("Cannot find any subscriptions in current account.");
        }

        AzureCliSubscription defaultSubscription = subscriptions.stream()
                .filter(AzureCliSubscription::isSelected).findFirst().orElse(subscriptions.get(0));

        this.entity.setEmail(defaultSubscription.getEmail());

        subscriptions = subscriptions.stream().filter(s -> StringUtils.equals(this.entity.getEmail(), s.getEmail()))
                .collect(Collectors.toList());

        this.entity.setEnvironment(defaultSubscription.getEnvironment());

        AzureCliTokenCredential azureCliCredential = new AzureCliTokenCredential();

        // use the tenant who has one or more subscriptions
        this.entity.setTenantIds(subscriptions.stream().map(Subscription::getTenantId).distinct().collect(Collectors.toList()));

        this.entity.setSubscriptions(subscriptions.stream().map(AzureCliAccount::toSubscription).collect(Collectors.toList()));

        // set initial selection of subscriptions
        this.entity.setSelectedSubscriptionIds(subscriptions.stream().filter(Subscription::isSelected)
                .map(Subscription::getId).distinct().collect(Collectors.toList()));

        this.entity.setTenantCredential(azureCliCredential);

        return azureCliCredential;

    }

    private static Subscription toSubscription(AzureCliSubscription s) {
        Subscription subscription = new Subscription();
        subscription.setId(s.getId());
        subscription.setName(s.getName());
        subscription.setSelected(s.isSelected());
        subscription.setTenantId(s.getTenantId());
        return subscription;
    }

    @Override
    public AuthMethod getMethod() {
        return AuthMethod.AZURE_CLI;
    }
}
