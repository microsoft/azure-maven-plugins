/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.TestKeys;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource.RemoteAwareResourceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudSku;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class SpringCloudClusterEntity extends RemoteAwareResourceEntity<SpringService> {
    private final String subscriptionId;
    private final String resourceGroup;
    private final String name;
    private final String id;

    SpringCloudClusterEntity(@Nonnull final SpringService resource) {
        this.remote = resource;
        final ResourceId resourceId = ResourceId.fromString(this.remote.id());
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
        this.name = resource.name();
        this.id = resource.id();
    }

    @Nullable
    public String getTestEndpoint() {
        return Optional.ofNullable(this.remote).map(SpringService::listTestKeys).map(TestKeys::primaryTestEndpoint).orElse(null);
    }

    @Nullable
    public String getTestKey() {
        return Optional.ofNullable(this.remote).map(SpringService::listTestKeys).map(TestKeys::primaryKey).orElse(null);
    }

    @Nonnull
    public SpringCloudSku getSku() {
        final SpringCloudSku dft = SpringCloudSku.builder()
                .capacity(25)
                .name("Basic")
                .tier("B0")
                .build();
        return Optional.ofNullable(this.remote).map(SpringService::sku).map(s -> SpringCloudSku.builder()
                .capacity(s.capacity())
                .name(s.name())
                .tier(s.tier())
                .build()).orElse(dft);
    }
}
