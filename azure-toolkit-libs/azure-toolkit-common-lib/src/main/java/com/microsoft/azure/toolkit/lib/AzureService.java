/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import java.util.List;

public interface AzureService {
    default List<Subscription> getSubscriptions() {
        return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

    @AzureOperation(name = "common|service.refresh", params = "this.name()", type = AzureOperation.Type.SERVICE)
    default void refresh() {
    }
}
