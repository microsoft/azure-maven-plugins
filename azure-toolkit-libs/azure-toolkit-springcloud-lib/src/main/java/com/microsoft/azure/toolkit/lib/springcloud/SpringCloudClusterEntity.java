/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.TestKeys;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureEntityManager.RemoteAwareResourceEntity;
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
        final ResourceId id = ResourceId.fromString(this.remote.id());
        this.resourceGroup = id.resourceGroupName();
        this.subscriptionId = id.subscriptionId();
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
