/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AzureEvent<T> {
    Object getSource();

    @Nonnull
    String getType();

    @Nullable
    T getPayload();
}
