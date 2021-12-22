/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.design.sp;

import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployments;
import com.microsoft.azure.toolkit.lib.design.AbstractAzResourceModule;

import javax.annotation.Nonnull;

public class SpringCloudDeploymentModule extends AbstractAzResourceModule<SpringCloudDeployment, SpringCloudApp, SpringAppDeployment> {

    public static final String NAME = "deployments";

    public SpringCloudDeploymentModule(@Nonnull SpringCloudApp parent) {
        super(NAME, parent);
    }

    @Override
    public SpringAppDeployments<?> getClient() {
        return this.parent.getRemote().deployments();
    }

    @Override
    protected SpringAppDeployment createResourceInAzure(String name, String resourceGroup, Object config) {
        return null;
    }

    @Override
    protected SpringAppDeployment updateResourceInAzure(SpringAppDeployment remote, Object config) {
        return null;
    }

    @Override
    protected SpringCloudDeployment createNewResource(String name, String resourceGroup, Object config) {
        return new SpringCloudDeployment(name, this);
    }

    @Nonnull
    protected SpringCloudDeployment wrap(SpringAppDeployment remote) {
        return new SpringCloudDeployment(remote, this);
    }
}
