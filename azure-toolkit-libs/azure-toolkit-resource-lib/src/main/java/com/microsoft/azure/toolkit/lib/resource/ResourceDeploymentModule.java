/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.Deployments;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ResourceDeploymentModule extends
    AbstractAzResourceModule<ResourceDeployment, ResourceGroup, Deployment> {

    public static final String NAME = "deployments";

    public ResourceDeploymentModule(@Nonnull ResourceGroup parent) {
        super(NAME, parent);
    }

    @Override
    public Deployments getClient() {
        return Optional.ofNullable(this.parent.getParent().getRemote()).map(ResourceManager::deployments).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected ResourceDeploymentDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "resource group is required.";
        return new ResourceDeploymentDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected ResourceDeploymentDraft newDraftForUpdate(@Nonnull ResourceDeployment origin) {
        return new ResourceDeploymentDraft(origin);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Stream<Deployment> loadResourcesFromAzure() {
        final ResourceManager manager = Objects.requireNonNull(this.parent.getParent().getRemote());
        return manager.deployments().listByResourceGroup(this.parent.getName()).stream();
    }

    @Nonnull
    protected ResourceDeployment newResource(@Nonnull Deployment r) {
        return new ResourceDeployment(r, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Resource deployment";
    }
}
