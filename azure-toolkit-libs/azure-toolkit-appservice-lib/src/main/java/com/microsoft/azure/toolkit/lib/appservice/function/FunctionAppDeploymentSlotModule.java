/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlots;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.IDeploymentSlotModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionAppDeploymentSlotModule extends AppServiceResourceModule<FunctionAppDeploymentSlot, FunctionApp, FunctionDeploymentSlot>
        implements IDeploymentSlotModule<FunctionAppDeploymentSlot, FunctionApp, FunctionDeploymentSlot> {

    public static final String NAME = "slots";

    public FunctionAppDeploymentSlotModule(@Nonnull FunctionApp parent) {
        super(NAME, parent);
    }

    @Override
    public FunctionDeploymentSlots getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(com.azure.resourcemanager.appservice.models.FunctionApp::deploymentSlots).orElse(null);
    }

    @Nonnull
    @Override
    protected FunctionAppDeploymentSlotDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new FunctionAppDeploymentSlotDraft(name, this);
    }

    @Nonnull
    @Override
    protected FunctionAppDeploymentSlotDraft newDraftForUpdate(@Nonnull FunctionAppDeploymentSlot origin) {
        return new FunctionAppDeploymentSlotDraft(origin);
    }

    @Nonnull
    @Override
    protected FunctionAppDeploymentSlot newResource(@Nonnull FunctionDeploymentSlot remote) {
        return new FunctionAppDeploymentSlot(remote, this);
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

    @Override
    protected List<String> loadResourceIdsFromAzure() {
        return Optional.ofNullable(getClient())
            .map(client -> client.list().stream().map(WebSiteBase::id).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }
}
