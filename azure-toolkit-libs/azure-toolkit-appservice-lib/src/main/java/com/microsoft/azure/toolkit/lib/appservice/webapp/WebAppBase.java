/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IOneDeploy;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;

public abstract class WebAppBase<T extends WebAppBase<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R extends com.azure.resourcemanager.appservice.models.WebAppBase>
    extends AppServiceAppBase<T, P, R> implements IOneDeploy {

    protected WebAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, resourceGroupName, module);
    }

    protected WebAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, module);
    }

    protected WebAppBase(@Nonnull T origin) {
        super(origin);
    }
}
