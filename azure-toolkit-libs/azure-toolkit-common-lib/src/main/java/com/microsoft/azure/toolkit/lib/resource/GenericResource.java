/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GenericResource extends AbstractAzResource<GenericResource, ResourceGroup, HasId> {

    @Nonnull
    @Getter
    private final ResourceId resourceId;
    @Nullable
    private AbstractAzResource<?, ?, ?> concrete;

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

    protected GenericResource(@Nonnull HasId remote, @Nonnull GenericResourceModule module) {
        super(remote.id(), module.getParent().getResourceGroupName(), module);
        this.resourceId = ResourceId.fromString(remote.id());
    }

    protected GenericResource(@Nonnull AbstractAzResource<?, ?, ?> concrete, @Nonnull GenericResourceModule module) {
        this(concrete::getId, module);
        this.concrete = concrete;
    }

    public synchronized AbstractAzResource<?, ?, ?> toConcreteResource() {
        if (Objects.isNull(this.concrete)) {
            this.concrete = Azure.az().getOrInitById(this.resourceId.id());
        }
        return Objects.isNull(concrete) ? this : concrete;
    }

    @Nullable
    @Override
    protected HasId refreshRemoteFromAzure(@Nonnull HasId remote) {
        return remote;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, GenericResource, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull HasId remote) {
        return Status.UNKNOWN;
    }

    public String getKind() {
        return this.remoteOptional().filter(r -> r instanceof com.azure.resourcemanager.resources.models.GenericResource)
            .map(r -> ((com.azure.resourcemanager.resources.models.GenericResource) r).kind())
            .orElseGet(() -> Objects.nonNull(this.concrete) ? this.concrete.getClass().getSimpleName() : "");
    }

    @Nonnull
    @Override
    public String getStatus() {
        final AbstractAzResource<?, ?, ?> concrete = this.toConcreteResource();
        return concrete instanceof GenericResource ? Status.UNKNOWN : concrete.getStatus();
    }

    @Override
    public boolean isDraftForCreating() {
        // don't use `toConcreteResource` because it will cause recursive calls.
        // toConcreteResource -> Azure.getOrInitById -> AzureAppService.getOrInitById -> GenericResource.getKind -> AbstractAzResource.getRemote -> isDraftForCreating
        if (Objects.nonNull(this.concrete) && !(this.concrete instanceof GenericResource)) {
            return this.concrete.isDraftForCreating();
        }
        return super.isDraftForCreating();
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
