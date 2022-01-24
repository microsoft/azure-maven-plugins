/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AzResourceModule<T extends AzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> {
    None NONE = new None();

    @Nonnull
    List<T> list();

    @Nullable
    T get(@Nonnull String name, String resourceGroup);

    boolean exists(@Nonnull String name, String resourceGroup);

    void delete(@Nonnull String name, String resourceGroup);

    T create(@Nonnull AzResource.Draft<T, R> draft);

    T update(@Nonnull AzResource.Draft<T, R> draft);

    void refresh();

    @Nonnull
    String getName();

    default String getFullResourceType() {
        return this.getParent().getFullResourceType() + "/" + this.getName();
    }

    String getResourceTypeName();

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
        protected AzResource.None newResource(@Nonnull Void unused) {
            return AzResource.NONE;
        }

        @Override
        public String getResourceTypeName() {
            return "NONE";
        }

        @Nonnull
        @Override
        public AzResource.None getParent() {
            return AzResource.NONE;
        }

        @Override
        protected Object getClient() {
            throw new AzureToolkitRuntimeException("not supported");
        }

        @Override
        public boolean equals(Object o) {
            return AzResourceModule.NONE == o;
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }
    }
}
