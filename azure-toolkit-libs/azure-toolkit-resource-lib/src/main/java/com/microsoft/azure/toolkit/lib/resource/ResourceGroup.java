/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ResourceGroup extends AbstractAzResource<ResourceGroup, ResourceGroupManager, com.azure.resourcemanager.resources.models.ResourceGroup>
    implements Removable {

    private final ResourceDeploymentModule deploymentModule;

    protected ResourceGroup(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ResourceGroupModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new ResourceDeploymentModule(this);
    }

    /**
     * copy constructor
     */
    protected ResourceGroup(@Nonnull ResourceGroup origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
    }

    protected ResourceGroup(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup remote, @Nonnull ResourceGroupModule module) {
        super(remote.name(), remote.name(), module);
        this.deploymentModule = new ResourceDeploymentModule(this);
        this.setRemote(remote);
    }

    @Override
    protected com.azure.resourcemanager.resources.models.ResourceGroup refreshRemote() {
        // ResourceGroup.refresh() doesn't work:
        // com.azure.core.management.exception.ManagementException: Status code 404,
        // "{"error":{"code":"ResourceGroupNotFound","message":"Resource group '${UUID}' could not be found."}}": Resource group '${UUID}' could not be found.
        final ResourceManager manager = Objects.requireNonNull(this.getParent().getRemote());
        return manager.resourceGroups().getByName(this.getName());
    }

    @Override
    public List<AzResourceModule<?, ResourceGroup, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    public ResourceDeploymentModule deployments() {
        return this.deploymentModule;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.resources.models.ResourceGroup remote) {
        return remote.provisioningState();
    }

    @Override
    public String status() {
        return this.getStatus();
    }

    @Nullable
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    public String getType() {
        return remoteOptional().map(Resource::type).orElse(null);
    }

    @Override
    public void remove() {
        this.delete();
    }

    @Nonnull
    public com.microsoft.azure.toolkit.lib.common.model.ResourceGroup toPojo() {
        return com.microsoft.azure.toolkit.lib.common.model.ResourceGroup.builder()
            .subscriptionId(this.getSubscriptionId())
            .id(this.getId())
            .name(this.getName())
            .region(Objects.requireNonNull(this.getRegion()).getName())
            .build();
    }
}
