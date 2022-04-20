/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudClusterModule extends AbstractAzResourceModule<SpringCloudCluster, SpringCloudServiceSubscription, SpringService> {

    public static final String NAME = "Spring";

    public SpringCloudClusterModule(@Nonnull SpringCloudServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nullable
    @Override
    public SpringServices getClient() {
        return Optional.ofNullable(this.parent.getRemote()).map(AppPlatformManager::springServices).orElse(null);
    }

    @Nonnull
    protected SpringCloudCluster newResource(@Nonnull SpringService r) {
        return new SpringCloudCluster(r, this);
    }

    @Nonnull
    protected SpringCloudCluster newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new SpringCloudCluster(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Spring Apps";
    }
}
