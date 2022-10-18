/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.resources.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class SpringCloudServiceSubscription extends AbstractAzServiceSubscription<SpringCloudServiceSubscription, AppPlatformManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final SpringCloudClusterModule clusterModule;

    SpringCloudServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureSpringCloud service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.clusterModule = new SpringCloudClusterModule(this);
    }

    SpringCloudServiceSubscription(@Nonnull AppPlatformManager remote, @Nonnull AzureSpringCloud service) {
        this(remote.subscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(clusterModule);
    }

    @Nonnull
    public SpringCloudClusterModule clusters() {
        return this.clusterModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.clusterModule.getName());
    }

    @Nonnull
    @Override
    public ResourceManager getResourceManager() {
        return Objects.requireNonNull(this.getRemote()).resourceManager();
    }
}

