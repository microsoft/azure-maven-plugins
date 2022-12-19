/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.ContainerAppsRevisions;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class RevisionModule extends AbstractAzResourceModule<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> {

    public static final String NAME = "revisions";

    public RevisionModule(@Nonnull ContainerApp parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Revision newResource(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision revision) {
        return new Revision(revision, this);
    }

    @Nonnull
    @Override
    protected Revision newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Revision(name, resourceGroupName, this);
    }

    @Override
    public void delete(@Nonnull String name, @Nullable String rgName) {
//        Optional.ofNullable(getClient()).ifPresent(client -> client.listRevisions());
    }

    @Nonnull
    @Override
    protected Stream<com.azure.resourcemanager.appcontainers.models.Revision> loadResourcesFromAzure() {
        final ContainerApp parent = this.getParent();
        return getClient().listRevisions(parent.getResourceGroupName(), parent.getName()).stream();
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appcontainers.models.Revision loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final ContainerApp parent = this.getParent();
        return getClient().getRevision(parent.getResourceGroupName(), parent.getName(), name);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<Revision, com.azure.resourcemanager.appcontainers.models.Revision> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new RevisionDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<Revision, com.azure.resourcemanager.appcontainers.models.Revision> newDraftForUpdate(@Nonnull Revision revision) {
        return new RevisionDraft(revision);
    }

    @Nullable
    @Override
    protected ContainerAppsRevisions getClient() {
        final ContainerAppsApiManager remote = getParent().getParent().getRemote();
        return Optional.ofNullable(remote).map(ContainerAppsApiManager::containerAppsRevisions).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Revisions";
    }
}
