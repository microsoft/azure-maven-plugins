/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.model;


import javax.annotation.Nonnull;

public abstract class AbstractEmulatableAzResource <T extends AbstractEmulatableAzResource<T, P, R>, P extends AzResource, R> extends AbstractAzResource<T, P, R> implements Emulatable {

    protected AbstractEmulatableAzResource(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public boolean exists() {
        return this.isEmulatorResource() ? this.getParent().exists() && this.remoteOptional().isPresent() : super.exists();
    }

    protected AbstractEmulatableAzResource(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, module);
    }

    protected AbstractEmulatableAzResource(@Nonnull AbstractAzResource<T, P, R> origin) {
        super(origin);
    }

    protected boolean isAuthRequired() {
        return !isEmulatorResource();
    }

    @Override
    public boolean isEmulatorResource() {
        return this.getParent() instanceof Emulatable && ((Emulatable) this.getParent()).isEmulatorResource();
    }
}
