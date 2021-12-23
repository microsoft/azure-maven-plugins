/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployments;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SpringCloudDeploymentModule extends AbstractAzResourceModule<SpringCloudDeployment, SpringCloudApp, SpringAppDeployment> {

    public static final String NAME = "deployments";

    public SpringCloudDeploymentModule(@Nonnull SpringCloudApp parent) {
        super(NAME, parent);
    }

    @Override
    public SpringAppDeployments<?> getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SpringApp::deployments).orElse(null);
    }

    @Override
    protected SpringCloudDeploymentDraft newDraft(@Nonnull String name, @Nonnull String resourceGroup) {
        return new SpringCloudDeploymentDraft(name, this);
    }

    @Nonnull
    protected SpringCloudDeployment newResource(@Nonnull SpringAppDeployment remote) {
        return new SpringCloudDeployment(remote, this);
    }
}
