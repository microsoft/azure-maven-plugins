/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlotBasic;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlots;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class FunctionAppDeploymentSlotModule extends AbstractAzResourceModule<FunctionAppDeploymentSlot, FunctionApp, WebSiteBase> {

    public static final String NAME = "slots";

    public FunctionAppDeploymentSlotModule(@Nonnull FunctionApp parent) {
        super(NAME, parent);
    }

    @Override
    public FunctionDeploymentSlots getClient() {
        return Optional.ofNullable(this.parent.getFullRemote()).map(com.azure.resourcemanager.appservice.models.FunctionApp::deploymentSlots).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected FunctionAppDeploymentSlotDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new FunctionAppDeploymentSlotDraft(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected FunctionAppDeploymentSlotDraft newDraftForUpdate(@Nonnull FunctionAppDeploymentSlot origin) {
        return new FunctionAppDeploymentSlotDraft(origin);
    }

    @Nonnull
    @Override
    protected FunctionAppDeploymentSlot newResource(@Nonnull WebSiteBase remote) {
        return new FunctionAppDeploymentSlot((FunctionDeploymentSlotBasic) remote, this);
    }

    @Nonnull
    @Override
    protected FunctionAppDeploymentSlot newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new FunctionAppDeploymentSlot(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Deployment slot";
    }
}
