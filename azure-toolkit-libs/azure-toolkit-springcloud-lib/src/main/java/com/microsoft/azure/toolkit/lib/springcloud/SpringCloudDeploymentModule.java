/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployments;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected SpringCloudDeploymentDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SpringCloudDeploymentDraft(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected SpringCloudDeploymentDraft newDraftForUpdate(@Nonnull SpringCloudDeployment origin) {
        return new SpringCloudDeploymentDraft(origin);
    }

    @Nonnull
    protected SpringCloudDeployment newResource(@Nonnull SpringAppDeployment remote) {
        return new SpringCloudDeployment(remote, this);
    }

    @Nonnull
    protected SpringCloudDeployment newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SpringCloudDeployment(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Spring Cloud app deployment";
    }
}
