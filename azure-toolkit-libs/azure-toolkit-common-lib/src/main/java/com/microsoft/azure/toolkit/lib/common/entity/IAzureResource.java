/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.entity;

import org.apache.commons.lang3.NotImplementedException;
import javax.annotation.Nullable;

public interface IAzureResource<T extends IAzureResourceEntity> extends IAzureBaseResource<IAzureBaseResource, IAzureBaseResource> {
    IAzureResource<T> refresh();

    T entity();

    default String name() {
        return this.entity().getName();
    }

    default String id() {
        return this.entity().getId();
    }

    @Nullable
    default IAzureBaseResource parent() {
        throw new NotImplementedException();
    }

    @Nullable
    default IAzureModule<IAzureBaseResource, IAzureBaseResource> module() {
        throw new NotImplementedException();
    }
}
