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

import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntity;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class SpringCloudAppEntity implements IAzureEntity {
    private final SpringCloudClusterEntity cluster;
    private final String name;
    AppResourceInner inner;

    private SpringCloudAppEntity(String name, SpringCloudClusterEntity cluster) {
        this.name = name;
        this.cluster = cluster;
    }

    private SpringCloudAppEntity(AppResourceInner resource, SpringCloudClusterEntity cluster) {
        this.inner = resource;
        this.name = resource.name();
        this.cluster = cluster;
    }

    @Nonnull
    public static SpringCloudAppEntity fromResource(final AppResourceInner resource, final SpringCloudClusterEntity cluster) {
        return new SpringCloudAppEntity(resource, cluster);
    }

    @Nonnull
    public static SpringCloudAppEntity fromName(final String name, final SpringCloudClusterEntity cluster) {
        return new SpringCloudAppEntity(name, cluster);
    }

    public boolean isPublic() {
        return this.inner.properties().publicProperty();
    }

    public String getApplicationUrl() {
        return this.inner.properties().url();
    }
}
