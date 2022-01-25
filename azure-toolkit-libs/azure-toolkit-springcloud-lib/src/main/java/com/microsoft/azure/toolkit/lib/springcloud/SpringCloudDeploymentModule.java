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
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

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
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected SpringCloudDeploymentDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new SpringCloudDeploymentDraft(name, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected SpringCloudDeploymentDraft newDraftForUpdate(@Nonnull SpringCloudDeployment origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new SpringCloudDeploymentDraft(origin);
    }

    @Nonnull
    protected SpringCloudDeployment newResource(@Nonnull SpringAppDeployment remote) {
        return new SpringCloudDeployment(remote, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Spring Cloud app deployment";
    }
}
