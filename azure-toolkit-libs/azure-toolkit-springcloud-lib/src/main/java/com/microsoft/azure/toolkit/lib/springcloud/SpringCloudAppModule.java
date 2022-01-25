/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringApps;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SpringCloudAppModule extends AbstractAzResourceModule<SpringCloudApp, SpringCloudCluster, SpringApp> {

    public static final String NAME = "apps";

    public SpringCloudAppModule(@Nonnull SpringCloudCluster parent) {
        super(NAME, parent);
    }

    @Override
    public SpringApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SpringService::apps).orElse(null);
    }

    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected SpringCloudAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new SpringCloudAppDraft(name, this);
    }

    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected SpringCloudAppDraft newDraftForUpdate(@Nonnull SpringCloudApp origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        return new SpringCloudAppDraft(origin);
    }

    @Nonnull
    protected SpringCloudApp newResource(@Nonnull SpringApp remote) {
        return new SpringCloudApp(remote, this);
    }

    @Override
    public String getResourceTypeName() {
        return "Spring Cloud app";
    }
}
