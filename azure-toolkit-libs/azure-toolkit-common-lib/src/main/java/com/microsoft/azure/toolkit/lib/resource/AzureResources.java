/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

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

    @Nullable
    public GenericResource getGenericResource(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final String rgName = id.resourceGroupName();
        final ResourceGroup rg = this.groups(id.subscriptionId()).get(rgName, rgName);
        return Optional.ofNullable(rg).map(ResourceGroup::genericResources)
            .map(r -> r.get(resourceId, rgName)).orElse(null);
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

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        return String.format("/subscriptions/%s", resourceName);
    }

    @Nullable
    public <E> E getById(@Nonnull String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        final String type = resourceId.resourceType();
        final ResourcesServiceSubscription subscription = Objects.requireNonNull(this.getOrDraft(resourceId.subscriptionId(), resourceGroup));
        if (type.equalsIgnoreCase("subscriptions")) {
            return (E) subscription;
        }
        final ResourceGroup group = subscription.resourceGroups().getOrDraft(resourceGroup, resourceGroup);
        if (type.equalsIgnoreCase(ResourceGroupModule.NAME)) {
            return (E) group;
        } else if (type.equalsIgnoreCase(ResourceDeploymentModule.NAME)) {
            return (E) group.deployments().getOrDraft(resourceId.name(), resourceGroup);
        } else {
            return (E) group.genericResources().get(id, resourceGroup);
        }
    }

    @Nullable
    public <E> E getOrInitById(@Nonnull String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        final String type = resourceId.resourceType();
        final ResourcesServiceSubscription subscription = Objects.requireNonNull(this.get(resourceId.subscriptionId(), resourceGroup));
        if (type.equals("subscriptions")) {
            return (E) subscription;
        }
        final ResourceGroup group = subscription.resourceGroups().get(resourceGroup, resourceGroup);
        if (type.equals(ResourceGroupModule.NAME)) {
            return (E) group;
        } else if (type.equals(ResourceDeploymentModule.NAME)) {
            return (E) group.deployments().getOrInit(resourceId.name(), resourceGroup);
        } else {
            return (E) group.genericResources().getOrInit(id, resourceGroup);
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Resource groups";
    }
}
