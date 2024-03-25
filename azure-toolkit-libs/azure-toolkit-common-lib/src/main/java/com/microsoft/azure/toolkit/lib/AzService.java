/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AzService {
    String getName();

    void refresh();

    @Nullable
    default <E> E getById(@Nonnull String id) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Nullable
    default <E> E getOrInitByConnectionString(@Nonnull String connectionString) {
        return null;
    }

    @Nullable
    default <E> E getOrInitById(@Nonnull String id) {
        throw new AzureToolkitRuntimeException("not supported");
    }
}
