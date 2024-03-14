/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.common.model;

import javax.annotation.Nonnull;

public abstract class AbstractEmulatableAzResourceModule<T extends AbstractEmulatableAzResource<T, P, R>, P extends AzResource, R>
    extends AbstractAzResourceModule<T, P, R> implements Emulatable {

    public AbstractEmulatableAzResourceModule(@Nonnull String name, @Nonnull P parent) {
        super(name, parent);
    }

    @Override
    protected boolean isAuthRequiredForListing() {
        return !isEmulatorResource() && super.isAuthRequiredForListing();
    }

    @Override
    public boolean isEmulatorResource() {
        return this.getParent() instanceof Emulatable && ((Emulatable) this.getParent()).isEmulatorResource();
    }
}
