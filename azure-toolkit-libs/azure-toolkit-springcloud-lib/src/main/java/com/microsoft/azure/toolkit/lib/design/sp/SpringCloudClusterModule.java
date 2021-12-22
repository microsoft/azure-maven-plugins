/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design.sp;

import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.design.AbstractAzResourceModule;

import javax.annotation.Nonnull;

public class SpringCloudClusterModule extends AbstractAzResourceModule<SpringCloudCluster, SpringCloudResourceManager, SpringService> {

    public static final String NAME = "Spring";

    public SpringCloudClusterModule(@Nonnull SpringCloudResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public SpringServices getClient() {
        return this.parent.getRemote().springServices();
    }

    @Override
    protected SpringService createResourceInAzure(String name, String resourceGroup, Object config) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected SpringService updateResourceInAzure(SpringService remote, Object config) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    protected SpringCloudCluster createNewResource(String name, String resourceGroup, Object config) {
        return new SpringCloudCluster(name, resourceGroup, this);
    }

    @Nonnull
    protected SpringCloudCluster wrap(SpringService r) {
        return new SpringCloudCluster(r, this);
    }
}
