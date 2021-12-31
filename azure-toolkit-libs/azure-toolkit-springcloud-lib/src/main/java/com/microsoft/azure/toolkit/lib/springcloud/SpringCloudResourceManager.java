/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class SpringCloudResourceManager extends AbstractAzResource<SpringCloudResourceManager, AzResource.None, AppPlatformManager> {
    @Nonnull
    private final String subscriptionId;
    private final SpringCloudClusterModule clusterModule;

    SpringCloudResourceManager(@Nonnull String subscriptionId, AzureSpringCloud service) {
        super(subscriptionId, AzResource.RESOURCE_GROUP_PLACEHOLDER, service);
        this.subscriptionId = subscriptionId;
        this.clusterModule = new SpringCloudClusterModule(this);
    }

    SpringCloudResourceManager(@Nonnull AppPlatformManager remote, AzureSpringCloud service) {
        this(remote.subscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, SpringCloudResourceManager, ?>> getSubModules() {
        return Collections.singletonList(clusterModule);
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

