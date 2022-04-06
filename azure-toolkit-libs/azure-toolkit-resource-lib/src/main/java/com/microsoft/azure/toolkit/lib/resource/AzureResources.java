/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Slf4j
public class AzureResources extends AbstractAzService<ResourcesServiceSubscription, ResourceManager> {
    public AzureResources() {
        super("Microsoft.Resources");
    }

    @Nonnull
    public ResourceGroupModule groups(@Nonnull String subscriptionId) {
        final ResourcesServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getGroupModule();
    }

    @Nonnull
    @Override
    protected ResourceManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        return AbstractAzServiceSubscription.getResourceManager(subscriptionId);
    }

    @Nonnull
    @Override
    protected ResourcesServiceSubscription newResource(@Nonnull ResourceManager remote) {
        return new ResourcesServiceSubscription(remote, this);
    }

    @Nullable
    public <E> E getById(@Nonnull String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        if (resourceId.resourceType().equals(ResourceDeploymentModule.NAME)) {
            final ResourcesServiceSubscription manager = Objects.requireNonNull(this.getOrDraft(resourceId.subscriptionId(), resourceGroup));
            final ResourceGroup group = manager.resourceGroups().getOrDraft(resourceGroup, resourceGroup);
            return (E) group.deployments().get(resourceId.name(), resourceGroup);
        }
        return super.getById(id);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Resource groups";
    }
}
