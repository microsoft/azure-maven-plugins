/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.models.ReplicaContainer;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Replica extends AbstractAzResource<Replica, Revision, com.azure.resourcemanager.appcontainers.models.Replica> {
    protected Replica(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ReplicaModule module) {
        super(name, resourceGroupName, module);
    }

    protected Replica(@Nonnull Replica origin) {
        super(origin);
    }

    protected Replica(@Nonnull com.azure.resourcemanager.appcontainers.models.Replica remote, @Nonnull ReplicaModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull com.azure.resourcemanager.appcontainers.models.Replica remote) {
        return Status.UNKNOWN;
    }

    public List<ReplicaContainer> getContainers() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.Replica::containers).orElse(Collections.emptyList());
    }
}
