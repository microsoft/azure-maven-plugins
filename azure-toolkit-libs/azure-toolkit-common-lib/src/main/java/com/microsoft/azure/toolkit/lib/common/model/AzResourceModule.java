/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface AzResourceModule<T extends AzResource> extends Refreshable, AzComponent {
    @Nonnull
    None NONE = new None();

    @Nonnull
    List<T> list();

    @Nullable
    default T get(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.get(id.name(), id.resourceGroupName());
    }

    @Nullable
    T get(@Nonnull String name, @Nullable String resourceGroup);

    @Nonnull
    T getOrDraft(@Nonnull String name, @Nullable String resourceGroup);

    boolean exists(@Nonnull String name, @Nullable String resourceGroup);

    void delete(@Nonnull String name, @Nullable String resourceGroup);

    @Nonnull
    T create(@Nonnull AzResource.Draft<T, ?> draft);

    @Nonnull
    T update(@Nonnull AzResource.Draft<T, ?> draft);

    void refresh();

    @Nonnull
    String getFullResourceType();

    @Nonnull
    String getResourceTypeName();

    @Nonnull
    String getServiceNameForTelemetry();

    @Nonnull
    String getSubscriptionId();

    @Getter
    final class None extends AbstractAzResourceModule<AzResource.None, AzResource.None, Void> {
        public None() {
            super(AzResource.None.NONE, null);
        }

        @Nonnull
        @Override
        protected AzResource.None newResource(@Nonnull Void unused) {
            return AzResource.NONE;
        }

        @Nonnull
        @Override
        protected AzResource.None newResource(@Nonnull String name, @Nullable String resourceGroupName) {
            return AzResource.NONE;
        }

        @Nonnull
        @Override
        public String getResourceTypeName() {
            return AzResource.None.NONE;
        }

        @Nonnull
        @Override
        public String getServiceNameForTelemetry() {
            return AzResource.None.NONE;
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
