/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AzResourceModule<T extends AzResource<T, P, ?>, P extends AzResource<P, ?, ?>> {
    None NONE = new None();

    @Nonnull
    List<T> list();

    @Nullable
    T get(@Nonnull String name, String resourceGroup);

    T create(@Nonnull String name, String resourceGroup, Object config);

    void update(@Nonnull String name, String resourceGroup, Object config);

    void delete(@Nonnull String name, String resourceGroup);

    void refresh();

    @Nonnull
    String getName();

    @Nonnull
    P getParent();

    @Nonnull
    default String getSubscriptionId() {
        return this.getParent().getSubscriptionId();
    }

    @Nonnull
    default String getId() {
        return String.format("%s/%s", this.getParent().getId(), this.getName());
    }

    @Getter
    final class None extends AbstractAzResourceModule<AzResource.None, AzResource.None, Void> {
        public None() {
            super("NONE", AzResource.NONE);
        }

        @Override
        protected Void createResourceInAzure(String name, String resourceGroup, Object config) {
            throw new AzureToolkitRuntimeException("not supported");
        }

        @Override
        protected Void updateResourceInAzure(Void remote, Object config) {
            throw new AzureToolkitRuntimeException("not supported");
        }

        @Override
        protected AzResource.None createNewResource(String name, String resourceGroup, Object config) {
            return AzResource.NONE;
        }

        @Override
        protected AzResource.None wrap(Void unused) {
            return AzResource.NONE;
        }

        @Override
        protected Object getClient() {
            throw new AzureToolkitRuntimeException("not supported");
        }
    }
}
