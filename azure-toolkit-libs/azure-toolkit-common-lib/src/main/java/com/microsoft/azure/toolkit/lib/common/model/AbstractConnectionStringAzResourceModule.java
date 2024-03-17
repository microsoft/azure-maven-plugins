/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractConnectionStringAzResourceModule<T> extends AbstractAzResourceModule<AbstractConnectionStringAzResource<T>, AzResource.None, String> {
    public static String CONNECTION_STRING_SUBSCRIPTION_ID = "<ConnectionStringResourcesSubscription>";
    public static String CONNECTION_STRING_RESOURCE_GROUP = "<ConnectionStringResourcesGroup>";

    public AbstractConnectionStringAzResourceModule(@NotNull final String name, final AzResource.None parent) {
        super(name, parent);
    }

    @Nonnull
    public AbstractConnectionStringAzResource<T> getOrInit(@Nonnull String connectionString) {
        return this.listCachedResources().stream().filter(r -> StringUtils.equalsIgnoreCase(connectionString, r.getConnectionString()))
            .findFirst().orElseGet(() -> {
                final AbstractConnectionStringAzResource<T> resource = this.newResource(connectionString);
                this.addResourceToLocal(resource.getId(), resource);
                return resource;
            });
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, String>> loadResourcePagesFromAzure() {
        final List<String> strings = this.resources.values().stream()
            .filter(Optional::isPresent).map(Optional::get).map(AbstractConnectionStringAzResource<T>::getConnectionString)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        return Collections.singletonList(new ItemPage<>(strings)).iterator();
    }

    @Nonnull
    @Override
    public String getFullResourceType() {
        return ResourceId.fromString(this.toResourceId("_FAKE_", CONNECTION_STRING_RESOURCE_GROUP)).fullResourceType();
    }

    @Nonnull
    @Override
    public String getSubscriptionId() {
        return CONNECTION_STRING_SUBSCRIPTION_ID;
    }

    @Nonnull
    @Override
    protected AbstractConnectionStringAzResource<T> newResource(@Nonnull final String name, @Nullable final String resourceGroupName) {
        throw new AzureToolkitRuntimeException("create `ConnectionStringStorageAccount` from `name` and `resourceGroupName` is not supported.");
    }
}
