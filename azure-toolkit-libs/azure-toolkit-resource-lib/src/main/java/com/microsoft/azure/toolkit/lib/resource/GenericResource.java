/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GenericResource extends AbstractAzResource<GenericResource, ResourceGroup, com.azure.resourcemanager.resources.models.GenericResource> {

    @Nonnull
    @Getter
    private final ResourceId resourceId;

    protected GenericResource(@Nonnull String resourceId, @Nonnull GenericResourceModule module) {
        super(resourceId, ResourceId.fromString(resourceId).resourceGroupName(), module);
        this.resourceId = ResourceId.fromString(resourceId);
    }

    /**
     * copy constructor
     */
    protected GenericResource(@Nonnull GenericResource origin) {
        super(origin);
        this.resourceId = origin.resourceId;
    }

    protected GenericResource(@Nonnull com.azure.resourcemanager.resources.models.GenericResource remote, @Nonnull GenericResourceModule module) {
        super(remote.id(), remote.resourceGroupName(), module);
        this.resourceId = ResourceId.fromString(remote.id());
        this.setRemote(remote);
    }

    public AbstractAzResource<?, ?, ?> toConcreteResource() {
        final AbstractAzResource<?, ?, ?> concrete = Azure.az().getOrInitById(this.resourceId.id());
        return Objects.isNull(concrete) ? this : concrete;
    }

    @Nonnull
    @Override
    protected com.azure.resourcemanager.resources.models.GenericResource refreshRemote(
        @Nonnull com.azure.resourcemanager.resources.models.GenericResource remote) {
        return remote;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, GenericResource, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull com.azure.resourcemanager.resources.models.GenericResource remote) {
        return Status.UNKNOWN;
    }

    public String getKind() {
        return this.remoteOptional().map(com.azure.resourcemanager.resources.models.GenericResource::kind).orElse("");
    }

    @Nonnull
    @Override
    public String getFullResourceType() {
        return this.resourceId.fullResourceType();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return this.getFullResourceType();
    }
}
