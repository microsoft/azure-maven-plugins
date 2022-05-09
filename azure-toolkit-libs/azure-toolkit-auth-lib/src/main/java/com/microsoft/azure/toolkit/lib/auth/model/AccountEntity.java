/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class AccountEntity {

    private AzureEnvironment environment;

    private AuthType type;

    private String clientId;

    private String email;

    @Setter(AccessLevel.NONE)
    private List<Subscription> subscriptions;

    private List<String> selectedSubscriptionIds;

    private List<String> tenantIds;

    private boolean available;

    private Throwable lastError;

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions.stream()
            .sorted(Comparator.comparing(s -> s.getName().toLowerCase()))
            .collect(Collectors.toList());
    }
}
