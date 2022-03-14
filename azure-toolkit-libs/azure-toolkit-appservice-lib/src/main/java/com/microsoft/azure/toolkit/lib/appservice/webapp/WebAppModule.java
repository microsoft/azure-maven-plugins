/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.WebApps;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Optional;

public class WebAppModule extends AbstractAzResourceModule<WebApp, AppServiceResourceManager, com.azure.resourcemanager.appservice.models.WebApp> {

    public static final String NAME = "sites";

    public WebAppModule(@Nonnull AppServiceResourceManager parent) {
        super(NAME, parent);
    }

    @Override
    public WebApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppServiceManager::webApps).orElse(null);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.draft_for_create.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected WebAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new WebAppDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.draft_for_update.resource|type",
        params = {"origin.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    protected WebAppDraft newDraftForUpdate(@Nonnull WebApp origin) {
        return new WebAppDraft(origin);
    }

    @Nonnull
    protected WebApp newResource(@Nonnull com.azure.resourcemanager.appservice.models.WebApp remote) {
        return new WebApp(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Web app";
    }
}
