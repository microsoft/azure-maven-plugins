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

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
public class SpringCloudClusterEntity implements IAzureEntity {
    private final String subscriptionId;
    private final String resourceGroup;
    private final String name;
    @Getter(AccessLevel.PACKAGE)
    private ServiceResourceInner inner;

    private SpringCloudClusterEntity(ServiceResourceInner resource) {
        this.inner = resource;
        final ResourceId id = ResourceId.fromString(this.inner.id());
        this.resourceGroup = id.resourceGroupName();
        this.subscriptionId = id.subscriptionId();
        this.name = resource.name();
    }

    public static SpringCloudClusterEntity fromResource(ServiceResourceInner resource) {
        return new SpringCloudClusterEntity(resource);
    }

    public SkuInner getSku() {
        return this.inner.sku();
    }
}
