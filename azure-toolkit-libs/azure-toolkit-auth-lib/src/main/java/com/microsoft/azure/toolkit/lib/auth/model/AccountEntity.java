/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.TenantCredential;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccountEntity {

    private AzureEnvironment environment;

    private String email;

    private boolean authenticated;

    private List<Subscription> subscriptions;

    private List<String> selectedSubscriptionIds;

    private List<String> tenantIds;

    private boolean available;

    private Throwable lastError;

    private TenantCredential tenantCredential;
}
