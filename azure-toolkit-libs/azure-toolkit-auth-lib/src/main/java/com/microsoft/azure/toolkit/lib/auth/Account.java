/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Account {
    @Getter
    private boolean authenticated;
    @Setter
    @Getter
    private AzureEnvironment environment;
    @Getter
    private List<SubscriptionEntity> selectedSubscriptions;

    private List<SubscriptionEntity> subscriptions;

    public Account logout() {
        this.authenticated = false;
        return this;
    }

    /**
     * @return the credential for specified subscription
     */
    public AzureTokenCredentials getCredential(String subscriptionId) {
        return null;
    }
}
