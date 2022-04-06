/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.plan;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlans;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class AppServicePlanModule extends AbstractAzResourceModule<AppServicePlan, AppServiceResourceManager, com.azure.resourcemanager.appservice.models.AppServicePlan> {

    public static final String NAME = "serverfarms";

    public AppServicePlanModule(@Nonnull AppServiceResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public AppServicePlans getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppServiceManager::appServicePlans).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected AppServicePlanDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new AppServicePlanDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected AppServicePlanDraft newDraftForUpdate(@Nonnull AppServicePlan origin) {
        return new AppServicePlanDraft(origin);
    }

    @Nonnull
    protected AppServicePlan newResource(@Nonnull com.azure.resourcemanager.appservice.models.AppServicePlan remote) {
        return new AppServicePlan(remote, this);
    }

    @Nonnull
    protected AppServicePlan newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new AppServicePlan(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "App Service plan";
    }
}
