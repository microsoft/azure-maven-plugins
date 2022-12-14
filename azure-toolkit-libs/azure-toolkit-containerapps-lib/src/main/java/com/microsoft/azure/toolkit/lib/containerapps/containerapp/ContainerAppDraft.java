/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerAppDraft extends ContainerApp implements AzResource.Draft<ContainerApp, com.azure.resourcemanager.appcontainers.models.ContainerApp> {
    @Getter
    @Nullable
    private final ContainerApp origin;

    protected ContainerAppDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected ContainerAppDraft(@Nonnull ContainerApp origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.ContainerApp createResourceInAzure() {
        return null;
    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.ContainerApp updateResourceInAzure(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp origin) {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Nullable
    @Override
    public ContainerApp getOrigin() {
        return this.origin;
    }
}
