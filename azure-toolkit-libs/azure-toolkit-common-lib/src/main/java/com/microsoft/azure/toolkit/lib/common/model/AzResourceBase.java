/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import javax.annotation.Nonnull;

public interface AzResourceBase {

    boolean exists();

    @Nonnull
    String getName();

    @Nonnull
    String getId();

    @Nonnull
    String getSubscriptionId();

    String getResourceGroupName();

    String getStatus();

    Subscription getSubscription();

    String getPortalUrl();
}
