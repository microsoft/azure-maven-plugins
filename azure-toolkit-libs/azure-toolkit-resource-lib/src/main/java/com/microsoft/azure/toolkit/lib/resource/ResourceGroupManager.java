/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class ResourceGroupManager extends AbstractAzResourceManager<ResourceGroupManager, ResourceManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final ResourceGroupModule groupModule;

    ResourceGroupManager(@Nonnull String subscriptionId, @Nonnull AzureResources service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.groupModule = new ResourceGroupModule(this);
    }

    ResourceGroupManager(@Nonnull ResourceManager remote, @Nonnull AzureResources service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, ResourceGroupManager, ?>> getSubModules() {
        return Collections.singletonList(groupModule);
    }

    @Nonnull
    public ResourceGroupModule resourceGroups() {
        return this.groupModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.groupModule.getName());
    }

    @Nonnull
    @Override
    public String getId() {
        return String.format("/subscriptions/%s/resourceGroups", this.subscriptionId);
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

