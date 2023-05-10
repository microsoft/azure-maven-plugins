/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class ReplicaContainerModule extends AbstractAzResourceModule<ReplicaContainer, Replica, com.azure.resourcemanager.appcontainers.models.ReplicaContainer> {
    public static final String NAME = "containers";

    public ReplicaContainerModule(@Nonnull Replica parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected ReplicaContainer newResource(@Nonnull com.azure.resourcemanager.appcontainers.models.ReplicaContainer remote) {
        return new ReplicaContainer(remote, this);
    }

    @Nonnull
    @Override
    protected ReplicaContainer newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ReplicaContainer(name, this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.appcontainers.models.ReplicaContainer>> loadResourcePagesFromAzure() {
        final Stream<com.azure.resourcemanager.appcontainers.models.ReplicaContainer> containers = Optional.of(this.getParent())
            .map(AbstractAzResource::getRemote)
            .map(c -> c.containers().stream())
            .orElse(Stream.empty());
        return Collections.singletonList(new ItemPage<>(containers)).iterator();
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appcontainers.models.ReplicaContainer loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.of(this.getParent())
            .map(AbstractAzResource::getRemote)
            .flatMap(r -> r.containers().stream().filter(c -> c.name().equalsIgnoreCase(name)).findAny())
            .orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Replica Container";
    }
}
