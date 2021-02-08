/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AccountEntity {

    private AuthType type;

    private String email;

    private boolean selected;

    private boolean authenticated;

    List<SubscriptionEntity> subscriptions;

    List<String> tenantIds;
}
