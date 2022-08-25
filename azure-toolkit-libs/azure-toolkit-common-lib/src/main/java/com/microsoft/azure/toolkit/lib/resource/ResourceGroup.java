/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceGroup extends AbstractAzResource<ResourceGroup, ResourcesServiceSubscription, com.azure.resourcemanager.resources.models.ResourceGroup>
    implements Deletable {

    private final ResourceDeploymentModule deploymentModule;
    private final GenericResourceModule resourceModule;

    protected ResourceGroup(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ResourceGroupModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new ResourceDeploymentModule(this);
        this.resourceModule = new GenericResourceModule(this);
    }

    /**
     * copy constructor
     */
    protected ResourceGroup(@Nonnull ResourceGroup origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
        this.resourceModule = origin.resourceModule;
    }

    protected ResourceGroup(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup remote, @Nonnull ResourceGroupModule module) {
        super(remote.name(), remote.name(), module);
        this.deploymentModule = new ResourceDeploymentModule(this);
        this.resourceModule = new GenericResourceModule(this);
    }

    @Nonnull
    @Override
    protected com.azure.resourcemanager.resources.models.ResourceGroup refreshRemoteFromAzure(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup remote) {
        // ResourceGroup.refresh() doesn't work:
        // com.azure.core.management.exception.ManagementException: Status code 404,
        // "{"error":{"code":"ResourceGroupNotFound","message":"Resource group '${UUID}' could not be found."}}": Resource group '${UUID}' could not be found.
        final ResourceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        return manager.resourceGroups().getByName(this.getName());
    }

    @Override
    public void delete() {
        final List<? extends AbstractAzResource<?, ?, ?>> localResources = this.genericResources().listCachedResources().stream()
            .map(GenericResource::toConcreteResource)
            .filter(r -> !(r instanceof GenericResource)).collect(Collectors.toList());
        localResources.forEach(r -> r.setStatus(Status.DELETING));
        try {
            super.delete();
        } catch (Throwable e) {
            localResources.forEach(r -> r.setStatus(Status.UNKNOWN));
            throw new AzureToolkitRuntimeException(e);
        }
        localResources.parallelStream().forEach(AbstractAzResource::delete);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ResourceGroup, ?>> getSubModules() {
        return Arrays.asList(deploymentModule, resourceModule);
    }

    public ResourceDeploymentModule deployments() {
        return this.deploymentModule;
    }

    public GenericResourceModule genericResources() {
        return this.resourceModule;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup remote) {
        return remote.provisioningState();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    public String getType() {
        return remoteOptional().map(Resource::type).orElse(null);
    }
}
