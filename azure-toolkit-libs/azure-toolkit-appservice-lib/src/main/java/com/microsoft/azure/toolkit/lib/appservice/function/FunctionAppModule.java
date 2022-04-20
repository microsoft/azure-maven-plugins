/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.azure.resourcemanager.appservice.models.FunctionApps;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class FunctionAppModule extends AbstractAzResourceModule<FunctionApp, AppServiceServiceSubscription, WebSiteBase> {

    public static final String NAME = "sites";

    public FunctionAppModule(@Nonnull AppServiceServiceSubscription parent) {
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
    protected FunctionApp newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new FunctionApp(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Function app";
    }

    @NotNull
    @Override
    public FunctionApp update(@NotNull AzResource.Draft<FunctionApp, WebSiteBase> draft) {
        log.debug("[{}]:update(draft:{})", this.getName(), draft);
        final FunctionApp resource = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.nonNull(resource) && Objects.nonNull(resource.getRemote())) {
            log.debug("[{}]:update->doModify(draft.updateResourceInAzure({}))", this.getName(), resource.getRemote());
            resource.doModify(() -> draft.updateResourceInAzure(Objects.requireNonNull(resource.getFullRemote())), AzResource.Status.UPDATING);
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", draft.getName()));
    }
}
