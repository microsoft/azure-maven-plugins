/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AzService {
    default List<Subscription> getSubscriptions() {
        return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
    }

    String getName();

    void refresh();

    @Nullable
    default <E> E getById(@Nonnull String id) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nullable
    default <E> E getOrInitById(@Nonnull String id) {
        throw new AzureToolkitRuntimeException("not supported");
    }
}
