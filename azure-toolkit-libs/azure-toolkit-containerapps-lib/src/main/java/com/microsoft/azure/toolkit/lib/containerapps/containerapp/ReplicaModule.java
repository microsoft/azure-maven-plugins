/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ContainerAppsRevisionReplicas;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

public class ReplicaModule extends AbstractAzResourceModule<Replica, Revision, com.azure.resourcemanager.appcontainers.models.Replica> {
    public static final String NAME = "replicas";

    public ReplicaModule(@Nonnull Revision parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Replica newResource(@Nonnull com.azure.resourcemanager.appcontainers.models.Replica remote) {
        return new Replica(remote, this);
    }

    @Nonnull
    @Override
    protected Replica newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Replica(name, this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.appcontainers.models.Replica>> loadResourcePagesFromAzure() {
        final ContainerApp containerApp = this.getParent().getParent();
        final Stream<com.azure.resourcemanager.appcontainers.models.Replica> replicaStream = Optional.ofNullable(this.getClient())
                .map(c -> c.listReplicas(containerApp.getResourceGroupName(), containerApp.getName(), parent.getName()).value().stream())
                .orElse(Stream.empty());
        return Collections.singletonList(new ItemPage<>(replicaStream)).iterator();
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appcontainers.models.Replica loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        return Optional.ofNullable(getClient()).map(client ->
                        client.getReplica(parent.getResourceGroupName(), parent.getParent().getName(), parent.getName(), name))
                .orElse(null);
    }

    @Nullable
    @Override
    protected ContainerAppsRevisionReplicas getClient() {
        final ContainerAppsApiManager remote = getParent().getParent().getParent().getRemote();
        return Optional.ofNullable(remote).map(ContainerAppsApiManager::containerAppsRevisionReplicas).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Replica";
    }
}
