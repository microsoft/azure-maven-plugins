/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.models.ContainerAppsRevisions;
import com.azure.resourcemanager.appcontainers.models.RevisionProvisioningState;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Revision extends AbstractAzResource<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> implements Deletable {
    protected Revision(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> module) {
        super(name, resourceGroupName, module);
    }

    protected Revision(@Nonnull Revision insight) {
        super(insight);
    }

    protected Revision(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision remote, @Nonnull RevisionModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    public void activate() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> Objects.requireNonNull(getClient()).activateRevision(parent.getResourceGroupName(), parent.getName(), getName()), AzResource.Status.STARTING);
    }

    public void deactivate() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> Objects.requireNonNull(getClient()).deactivateRevision(parent.getResourceGroupName(), parent.getName(), getName()), AzResource.Status.STARTING);
    }

    public void restart() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> Objects.requireNonNull(getClient()).restartRevision(parent.getResourceGroupName(), parent.getName(), getName()), AzResource.Status.STARTING);
    }

    @Nullable
    public String getFqdn() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.fqdn()).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public boolean isActive() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.active()).orElse(false);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision remote) {
        return remote.provisioningState() == RevisionProvisioningState.PROVISIONED ?
                remote.healthState().toString() : remote.provisioningState().toString();
    }

    @Nullable
    private ContainerAppsRevisions getClient() {
        return ((RevisionModule) this.getModule()).getClient();
    }
}
