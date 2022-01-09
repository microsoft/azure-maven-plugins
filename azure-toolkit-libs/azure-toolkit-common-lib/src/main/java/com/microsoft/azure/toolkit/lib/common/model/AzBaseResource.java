/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;

import javax.annotation.Nonnull;

public interface AzBaseResource {
    boolean exists();

    @Nonnull
    String getName();

    @Nonnull
    String getId();

    @Nonnull
    String getSubscriptionId();

    String getResourceGroupName();

    String getStatus();

    default Subscription getSubscription() {
        return Azure.az(IAzureAccount.class).account().getSubscription(this.getSubscriptionId());
    }

    default String getPortalUrl() {
        final IAccount account = Azure.az(IAzureAccount.class).account();
        Subscription subscription = account.getSubscription(this.getSubscriptionId());
        return String.format("%s/#@%s/resource%s", account.portalUrl(), subscription.getTenantId(), this.getId());
    }
}
