/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.azure.resourcemanager.appservice.models.FunctionApps;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Optional;

public class FunctionAppModule extends AbstractAzResourceModule<FunctionApp, AppServiceResourceManager, WebSiteBase> {

    public static final String NAME = "sites";

    public FunctionAppModule(@Nonnull AppServiceResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public FunctionApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppServiceManager::functionApps).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected FunctionAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new FunctionAppDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected FunctionAppDraft newDraftForUpdate(@Nonnull FunctionApp origin) {
        return new FunctionAppDraft(origin);
    }

    @Nonnull
    protected FunctionApp newResource(@Nonnull WebSiteBase remote) {
        return new FunctionApp((FunctionAppBasic) remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Function app";
    }
}
