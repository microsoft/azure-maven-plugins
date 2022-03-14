/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AzResourceModule<T extends AzResource<T, P, R>, P extends AzResource<P, ?, ?>, R> {
    @Nonnull
    None NONE = new None();

    @Nonnull
    List<T> list();

    @Nullable
    T get(@Nonnull String name, @Nullable String resourceGroup);

    boolean exists(@Nonnull String name, @Nullable String resourceGroup);

    void delete(@Nonnull String name, @Nullable String resourceGroup);

    @Nonnull
    T create(@Nonnull AzResource.Draft<T, R> draft);

    @Nonnull
    T update(@Nonnull AzResource.Draft<T, R> draft);

    void refresh();

    @Nonnull
    String getName();

    @Nonnull
    default String getFullResourceType() {
        return this.getParent().getFullResourceType() + "/" + this.getName();
    }

    @Nonnull
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

        @Nonnull
        @Override
        protected AzResource.None newResource(@Nonnull Void unused) {
            return AzResource.NONE;
        }

        @Nonnull
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
        @Contract(value = "null -> false", pure = true)
        public boolean equals(Object o) {
            return o instanceof None;
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }
    }
}
