/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class SpringCloudResourceManager extends AbstractAzResource<SpringCloudResourceManager, AzResource.None, AppPlatformManager> {
    @Nonnull
    private final String subscriptionId;
    private final String id;
    private final SpringCloudClusterModule clusterModule;

    SpringCloudResourceManager(@Nonnull String subscriptionId, AzureSpringCloud service) {
        super(subscriptionId, AzResource.RESOURCE_GROUP_PLACEHOLDER, service);
        this.subscriptionId = subscriptionId;
        this.id = String.format("/subscriptions/%s/resourceGroups/%s/providers/%s",
            this.getSubscriptionId(), AzResource.RESOURCE_GROUP_PLACEHOLDER, service.getName());
        this.clusterModule = new SpringCloudClusterModule(this);
    }

    SpringCloudResourceManager(@Nonnull AppPlatformManager remote, AzureSpringCloud service) {
        this(remote.subscriptionId(), service);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull AppPlatformManager remote) {
        return Status.UNKNOWN;
    }

    public SpringCloudClusterModule clusters() {
        return this.clusterModule;
    }
}

