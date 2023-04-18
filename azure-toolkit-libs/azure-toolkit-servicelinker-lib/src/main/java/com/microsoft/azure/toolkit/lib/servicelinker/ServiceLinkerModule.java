/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.servicelinker;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.servicelinker.ServiceLinkerManager;
import com.azure.resourcemanager.servicelinker.models.LinkerResource;
import com.azure.resourcemanager.servicelinker.models.Linkers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class ServiceLinkerModule extends AbstractAzResourceModule<ServiceLinker, Consumer, LinkerResource> {
    public static final String NAME = "linkers";
    private final String targetResourceId;
    public ServiceLinkerModule(String targetResourceId, Consumer parent) {
        super(NAME, parent);
        this.targetResourceId = targetResourceId;
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, LinkerResource>> loadResourcePagesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list(targetResourceId).iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    protected LinkerResource loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        assert StringUtils.isNoneBlank(resourceGroup) : "resource group can not be empty";
        return Optional.ofNullable(this.getClient()).map(linkers -> linkers.get(targetResourceId, name)).orElse(null);
    }

    @Nonnull
    @Override
    protected ServiceLinker newResource(@Nonnull LinkerResource remote) {
        return new ServiceLinker(remote, this);
    }

    @Nonnull
    @Override
    protected ServiceLinker newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new ServiceLinker(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        Optional.ofNullable(this.getClient()).ifPresent(linkers -> {
            final ResourceId resource = ResourceId.fromString(resourceId);
            linkers.deleteByResourceGroup(targetResourceId, resource.name());
        });
    }

    @Nullable
    @Override
    protected Linkers getClient() {
        return Optional.ofNullable(this.parent.getManager()).map(ServiceLinkerManager::linkers).orElse(null);
    }
}
