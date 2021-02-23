/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AccountEntity {

    private AuthMethod method;

    private AzureEnvironment environment;

    private String email;

    private boolean selected;

    private boolean authenticated;

    private List<SubscriptionEntity> subscriptions;

    private List<SubscriptionEntity> selectedSubscriptions;

    private List<String> tenantIds;

    private Throwable error;

    private List<String> selectedSubscriptionIds;

    private ICredentialProvider credentialBuilder;
}
