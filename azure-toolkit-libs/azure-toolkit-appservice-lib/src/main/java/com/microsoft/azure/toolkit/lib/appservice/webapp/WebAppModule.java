/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.WebApps;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class WebAppModule extends AppServiceResourceModule<WebApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.WebApp> {

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
    protected WebApp newResource(@Nonnull com.azure.resourcemanager.appservice.models.WebApp remote) {
        return new WebApp(remote, this);
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

    @Override
    protected List<String> loadResourceIdsFromAzure() {
        return Optional.ofNullable(getClient())
            .map(client -> client.list().stream().map(WebSiteBase::id).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }
}
