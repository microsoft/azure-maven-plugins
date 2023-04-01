/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.appservice.models.WebApps;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@CustomLog
public class WebAppModule extends AbstractAzResourceModule<WebApp, AppServiceServiceSubscription, WebSiteBase> {

    public static final String NAME = "sites";

    public WebAppModule(@Nonnull AppServiceServiceSubscription parent) {
        super(NAME, parent);
    }

    @Override
    public WebApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppServiceManager::webApps).orElse(null);
    }

    @Nonnull
    @Override
    protected WebAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new WebAppDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected WebAppDraft newDraftForUpdate(@Nonnull WebApp origin) {
        return new WebAppDraft(origin);
    }

    @Nonnull
    @Override
    protected WebApp newResource(@Nonnull WebSiteBase remote) {
        return new WebApp((WebAppBasic) remote, this);
    }

    @Nonnull
    @Override
    protected WebApp newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new WebApp(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Web app";
    }

    @Nonnull
    @Override
    public WebApp update(@Nonnull AzResource.Draft<WebApp, ?> d) {
        final AzResource.Draft<FunctionApp, WebSiteBase> draft = this.cast(d);
        log.debug(String.format("[%s]:update(draft:%s)", this.getName(), draft));
        final WebApp resource = this.get(draft.getName(), draft.getResourceGroupName());
        if (Objects.nonNull(resource) && Objects.nonNull(resource.getRemote())) {
            log.debug(String.format("[%s]:update->doModify(draft.updateResourceInAzure(%s))", this.getName(), resource.getRemote()));
            resource.doModify(() -> draft.updateResourceInAzure(Objects.requireNonNull(resource.getFullRemote())), AzResource.Status.UPDATING);
            return resource;
        }
        throw new AzureToolkitRuntimeException(String.format("resource \"%s\" doesn't exist", draft.getName()));
    }
}
