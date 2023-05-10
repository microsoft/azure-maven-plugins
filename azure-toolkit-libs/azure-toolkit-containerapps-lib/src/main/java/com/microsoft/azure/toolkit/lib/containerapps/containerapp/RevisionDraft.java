/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RevisionDraft extends Revision implements AzResource.Draft<Revision, com.azure.resourcemanager.appcontainers.models.Revision> {
    @Getter
    @Nullable
    private final Revision origin;

    protected RevisionDraft(@Nonnull String name, @Nonnull AbstractAzResourceModule<Revision, ContainerApp, com.azure.resourcemanager.appcontainers.models.Revision> module) {
        super(name, module);
        this.origin = null;
    }

    protected RevisionDraft(@Nonnull Revision origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {

    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.Revision createResourceInAzure() {
        return null;
    }

    @Nonnull
    @Override
    public com.azure.resourcemanager.appcontainers.models.Revision updateResourceInAzure(@Nonnull com.azure.resourcemanager.appcontainers.models.Revision origin) {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }
}
