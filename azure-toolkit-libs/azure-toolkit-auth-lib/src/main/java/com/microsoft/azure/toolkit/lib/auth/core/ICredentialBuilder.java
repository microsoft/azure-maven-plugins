/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.toolkit.lib.auth.model.SubscriptionEntity;

public interface ICredentialBuilder {

    TokenCredential getCredentialWrapperForSubscription(SubscriptionEntity subscription);

    TokenCredential getCredentialForTenant(String tenantId);

    TokenCredential getCredentialForListingTenants();
}
