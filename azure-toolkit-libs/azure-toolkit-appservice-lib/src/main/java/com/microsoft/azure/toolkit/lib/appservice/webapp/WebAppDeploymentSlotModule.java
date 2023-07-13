/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.DeploymentSlots;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.IDeploymentSlotModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WebAppDeploymentSlotModule extends AppServiceResourceModule<WebAppDeploymentSlot, WebApp, DeploymentSlot>
        implements IDeploymentSlotModule<WebAppDeploymentSlot, WebApp, DeploymentSlot> {

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
    protected WebAppDeploymentSlotDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        return new WebAppDeploymentSlotDraft(name, this);
    }

    @Nonnull
    @Override
    protected WebAppDeploymentSlotDraft newDraftForUpdate(@Nonnull WebAppDeploymentSlot origin) {
        return new WebAppDeploymentSlotDraft(origin);
    }

    @Nonnull
    @Override
    protected WebAppDeploymentSlot newResource(@Nonnull DeploymentSlot remote) {
        return new WebAppDeploymentSlot(remote, this);
    }

    @Nonnull
    @Override
    protected WebAppDeploymentSlot newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new WebAppDeploymentSlot(name, this);
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
