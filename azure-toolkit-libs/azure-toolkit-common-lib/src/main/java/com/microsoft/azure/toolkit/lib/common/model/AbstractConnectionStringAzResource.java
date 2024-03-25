/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Getter;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResourceModule.CONNECTION_STRING_RESOURCE_GROUP;

public abstract class AbstractConnectionStringAzResource<T> extends AbstractAzResource<AbstractConnectionStringAzResource<T>, AzResource.None, String> {
    @Nonnull
    @Getter
    protected final String connectionString;

    protected AbstractConnectionStringAzResource(@Nonnull final String connectionString, final String name, final AbstractAzResourceModule<AbstractConnectionStringAzResource<T>, AzResource.None, String> module) {
        super(name, CONNECTION_STRING_RESOURCE_GROUP, module);
        this.connectionString = connectionString;
    }

    @Override
    public ResourceGroup getResourceGroup() {
        return null;
    }
}
