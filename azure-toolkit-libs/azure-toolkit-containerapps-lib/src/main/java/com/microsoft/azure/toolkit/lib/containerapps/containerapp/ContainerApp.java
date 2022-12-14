/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ContainerApp extends AbstractAzResource<ContainerApp, AzureContainerAppsServiceSubscription, com.azure.resourcemanager.appcontainers.models.ContainerApp> implements Deletable {
    private final RevisionModule revisionModule;

    protected ContainerApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppModule module) {
        super(name, resourceGroupName, module);
        this.revisionModule = new RevisionModule(this);
    }

    protected ContainerApp(@Nonnull ContainerApp insight) {
        super(insight);
        this.revisionModule = insight.revisionModule;
    }

    protected ContainerApp(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote, @Nonnull ContainerAppModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.revisionModule = new RevisionModule(this);
    }

    public RevisionModule revisions() {
        return this.revisionModule;
    }

    public RevisionMode revisionModel() {
        return Optional.ofNullable(getRemote())
                .map(remote -> remote.configuration())
                .map(configuration -> configuration.activeRevisionsMode())
                .map(mode -> RevisionMode.fromString(mode.toString()))
                .orElse(RevisionMode.UNKNOWN);
    }

    @Nullable
    public String getLatestRevisionFqdn() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.latestRevisionFqdn()).orElse(null);
    }

    @Nullable
    public String getManagedEnvironmentId() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.managedEnvironmentId()).orElse(null);
    }

    @Nullable
    public String getEnvironmentId() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.environmentId()).orElse(null);
    }

    @Nullable
    public String getLatestRevisionName() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.latestRevisionName()).orElse(null);
    }

    @Nullable
    public Revision getLatestRevision() {
        return Optional.ofNullable(getLatestRevisionName())
                .map(name -> this.revisions().get(name, this.getResourceGroupName())).orElse(null);
    }

    public void activate() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).activate(), AzResource.Status.STARTING);
    }

    public void deactivate() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).deactivate(), AzResource.Status.STARTING);
    }

    public void restart() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).restart(), AzResource.Status.STARTING);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.revisionModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote) {
        return remote.provisioningState().toString();
    }
}
