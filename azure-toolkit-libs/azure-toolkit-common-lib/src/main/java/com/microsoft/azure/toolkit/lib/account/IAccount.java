/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.account;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import java.util.List;

public interface IAccount {

    String getPortalUrl();

    String getUsername();

    String getClientId();

    List<? extends Subscription> getSubscriptions();

    List<Subscription> getSelectedSubscriptions();

    Subscription getSubscription(String subscriptionId);

    AzureEnvironment getEnvironment();

    TokenCredential getTokenCredential(String subscriptionId);
}
