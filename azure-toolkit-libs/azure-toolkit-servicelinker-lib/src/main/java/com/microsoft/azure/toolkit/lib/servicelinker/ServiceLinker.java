/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.servicelinker;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.servicelinker.models.AzureResource;
import com.azure.resourcemanager.servicelinker.models.LinkerResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ServiceLinker extends AbstractAzResource<ServiceLinker, Consumer, LinkerResource> implements Deletable {
    @Nullable
    @Getter
    private String targetServiceId = null;
    protected ServiceLinker(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<ServiceLinker, Consumer, LinkerResource> module) {
        super(name, resourceGroupName, module);
    }

    protected ServiceLinker(@Nonnull ServiceLinker origin) {
        super(origin);
    }

    protected ServiceLinker(@Nonnull LinkerResource remote, @Nonnull ServiceLinkerModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable LinkerResource newRemote, @Nullable LinkerResource oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        Optional.ofNullable(newRemote).ifPresent(remote -> {
            if (remote.targetService() instanceof AzureResource) {
                this.targetServiceId = ((AzureResource) remote.targetService()).id();
            }
        });
    }

    @Override
    public void invalidateCache() {
        super.invalidateCache();
        this.targetServiceId = null;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull LinkerResource remote) {
        return remote.provisioningState();
    }
}
