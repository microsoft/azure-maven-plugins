/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.models.ContainerAppsRevisions;
import com.azure.resourcemanager.appcontainers.models.RevisionHealthState;
import com.azure.resourcemanager.appcontainers.models.RevisionProvisioningState;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class Revision extends AbstractAzResource<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> implements Deletable {
    @Getter
    private final ReplicaModule replicaModule;
    protected Revision(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> module) {
        super(name, resourceGroupName, module);
        this.replicaModule = new ReplicaModule(this);
    }

    protected Revision(@Nonnull Revision insight) {
        super(insight);
        this.replicaModule = insight.replicaModule;
    }

    protected Revision(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision remote, @Nonnull RevisionModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.replicaModule = new ReplicaModule(this);
    }

    @AzureOperation(name = "azure/containerapps.activate_revision.revision", params = {"this.getName()"})
    public void activate() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> {
            Objects.requireNonNull(getClient()).activateRevision(parent.getResourceGroupName(), parent.getName(), getName());
            this.getModule().refresh();
        }, Status.ACTIVATING);
    }

    @AzureOperation(name = "azure/containerapps.deactivate_revision.revision", params = {"this.getName()"})
    public void deactivate() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> {
            Objects.requireNonNull(getClient()).deactivateRevision(parent.getResourceGroupName(), parent.getName(), getName());
            this.getModule().refresh();
        }, Status.DEACTIVATING);
    }

    @AzureOperation(name = "azure/containerapps.restart_revision.revision", params = {"this.getName()"})
    public void restart() {
        final ContainerApp parent = this.getParent();
        this.doModify(() -> {
            Objects.requireNonNull(getClient()).restartRevision(parent.getResourceGroupName(), parent.getName(), getName());
            this.getModule().refresh();
        }, Status.RESTARTING);
    }

    @Nullable
    public OffsetDateTime getCreatedTime() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.Revision::createdTime).orElse(null);
    }

    @Nullable
    public OffsetDateTime getLastActiveTime() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.Revision::lastActiveTime).orElse(null);
    }

    @Nullable
    public String getFqdn() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.Revision::fqdn).orElse(null);
    }

    @Nullable
    public String getProvisioningState() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.provisioningState().toString()).orElse(null);
    }

    @Nullable
    public Integer getTrafficWeight() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.innerModel().trafficWeight()).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    public boolean isActive() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.Revision::active).orElse(false);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision remote) {
        final RevisionProvisioningState provisioningState = remote.provisioningState();
        if (provisioningState == RevisionProvisioningState.PROVISIONED) {
            final RevisionHealthState healthState = remote.healthState();
            if (healthState == RevisionHealthState.HEALTHY && !remote.active()) {
                return Status.STOPPED;
            }
            return healthState.toString();
        }
        return provisioningState.toString();
    }

    public List<Replica> getReplicas() {
        return replicaModule.list();
    }

    @Nullable
    private ContainerAppsRevisions getClient() {
        return ((RevisionModule) this.getModule()).getClient();
    }
}
