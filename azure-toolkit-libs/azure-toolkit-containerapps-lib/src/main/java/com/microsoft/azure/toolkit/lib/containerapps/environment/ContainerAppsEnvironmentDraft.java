/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerAppsEnvironmentDraft extends ContainerAppsEnvironment implements AzResource.Draft<ContainerAppsEnvironment, ManagedEnvironment>{

    protected ContainerAppsEnvironmentDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppsEnvironmentModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    public ManagedEnvironment createResourceInAzure() {
        return null;
    }

    @Nonnull
    @Override
    public ManagedEnvironment updateResourceInAzure(@Nonnull ManagedEnvironment origin) {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Nullable
    @Override
    public ContainerAppsEnvironment getOrigin() {
        return null;
    }
}
