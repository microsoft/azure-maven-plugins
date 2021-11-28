/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nullable;

public interface IAzureModule<T extends IAzureBaseResource, P extends IAzureBaseResource> {

    @Nullable
    default P getParent() {
        return null;
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

    @AzureOperation(name = "common.refresh_service", params = "this.name()", type = AzureOperation.Type.SERVICE)
    default void refresh() {
    }
}

