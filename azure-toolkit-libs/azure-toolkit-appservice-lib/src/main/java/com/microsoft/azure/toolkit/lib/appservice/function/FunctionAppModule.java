/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.FunctionApps;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceModule;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class FunctionAppModule extends AppServiceResourceModule<FunctionApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.FunctionApp> {

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
    protected FunctionAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new FunctionAppDraft(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected FunctionAppDraft newDraftForUpdate(@Nonnull FunctionApp origin) {
        return new FunctionAppDraft(origin);
    }

    @Nonnull
    protected FunctionApp newResource(@Nonnull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        return new FunctionApp(remote, this);
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

    @Override
    protected List<String> loadResourceIdsFromAzure() {
        return Optional.ofNullable(getClient())
            .map(client -> client.list().stream().map(WebSiteBase::id).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }
}
