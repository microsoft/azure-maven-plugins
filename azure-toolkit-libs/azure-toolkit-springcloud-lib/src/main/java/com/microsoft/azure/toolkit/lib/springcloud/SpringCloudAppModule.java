/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringApps;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class SpringCloudAppModule extends AbstractAzResourceModule<SpringCloudApp, SpringCloudCluster, SpringApp> {

    public static final String NAME = "apps";

    public SpringCloudAppModule(@Nonnull SpringCloudCluster parent) {
        super(NAME, parent);
    }

    @Override
    public SpringApps getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(SpringService::apps).orElse(null);
    }

    @Nonnull
    @Override
    protected SpringCloudAppDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new SpringCloudAppDraft(name, this);
    }

    @Nonnull
    @Override
    protected SpringCloudAppDraft newDraftForUpdate(@Nonnull SpringCloudApp origin) {
        return new SpringCloudAppDraft(origin);
    }

    @Nonnull
    protected SpringCloudApp newResource(@Nonnull SpringApp remote) {
        return new SpringCloudApp(remote, this);
    }

    @Nonnull
    protected SpringCloudApp newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SpringCloudApp(name, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Spring app";
    }
}
