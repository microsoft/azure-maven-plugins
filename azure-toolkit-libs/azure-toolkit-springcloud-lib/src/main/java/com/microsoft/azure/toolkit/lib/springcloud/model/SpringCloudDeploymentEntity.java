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

package com.microsoft.azure.toolkit.lib.springcloud.model;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
import com.microsoft.azure.toolkit.lib.common.IAzureEntity;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.List;

@Getter
public class SpringCloudDeploymentEntity implements IAzureEntity {
    private SpringCloudAppEntity app;
    private String name;
    private SkuInner sku;
    DeploymentResourceInner inner;

    @Nonnull
    public static SpringCloudDeploymentEntity fromResource(DeploymentResourceInner resource) {
        return null;
    }

    public DeploymentResourceInner inner() {
        return this.inner;
    }

    public SpringCloudDeploymentEntity inner(DeploymentResourceInner inner) {
        this.inner = inner;
        return this;
    }

    public DeploymentResourceStatus getStatus() {
        return this.inner.properties().status();
    }

    public List<DeploymentInstance> getInstances() {
        return this.inner.properties().instances();
    }

    public Boolean isActive() {
        return this.inner.properties().active();
    }
}
