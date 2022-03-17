/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlots;
import com.azure.resourcemanager.appservice.models.WebDeploymentSlotBasic;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class WebAppDeploymentSlotModule extends AbstractAzResourceModule<WebAppDeploymentSlot, WebApp, DeploymentSlot> {

    public static final String NAME = "slots";

    public WebAppDeploymentSlotModule(@Nonnull WebApp parent) {
        super(NAME, parent);
    }

    @Override
    public DeploymentSlots getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(com.azure.resourcemanager.appservice.models.WebApp::deploymentSlots).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected WebAppDeploymentSlotDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new WebAppDeploymentSlotDraft(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected WebAppDeploymentSlotDraft newDraftForUpdate(@Nonnull WebAppDeploymentSlot origin) {
        return new WebAppDeploymentSlotDraft(origin);
    }

    @Nonnull
    protected WebAppDeploymentSlot newResource(@Nonnull DeploymentSlot remote) {
        return new WebAppDeploymentSlot(remote, this);
    }

    @Nonnull
    protected WebAppDeploymentSlot newResourceInner(@Nonnull Object r) {
        if (r instanceof WebDeploymentSlotBasic) {
            return new WebAppDeploymentSlot((WebDeploymentSlotBasic) r, this);
        }
        return super.newResourceInner(r);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Deployment slot";
    }
}
