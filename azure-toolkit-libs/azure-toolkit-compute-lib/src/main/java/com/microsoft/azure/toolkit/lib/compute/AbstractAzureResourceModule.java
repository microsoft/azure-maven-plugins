/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.compute;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractAzureResourceModule<T extends IAzureBaseResource> extends SubscriptionScoped<AbstractAzureResourceModule<T>>
        implements AzureService<T, IAzureBaseResource> {

    public AbstractAzureResourceModule(@NotNull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator,
                                       @Nullable List<Subscription> subscriptions) {
        super(creator, subscriptions);
    }

    public AbstractAzureResourceModule(@NotNull Function<List<Subscription>, AbstractAzureResourceModule<T>> creator) {
        super(creator);
    }

    public List<T> list() {
        return getSubscriptions().stream().parallel()
                .flatMap(subscription -> list(subscription.getId()).stream())
                .collect(Collectors.toList());
    }

    @Nonnull
    public T get(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        return get(resourceId.subscriptionId(), resourceId.resourceGroupName(), resourceId.name());
    }

    @Nonnull
    public T get(@Nonnull final String resourceGroup, @Nonnull final String name) {
        return get(getDefaultSubscription().getId(), resourceGroup, name);
    }

    public abstract List<T> list(@Nonnull final String subscriptionId);

    @Nonnull
    public abstract T get(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name);
}
