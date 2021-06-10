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

import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.resources.fluentcore.arm.models.ExternalChildResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Getter
public class SpringCloudAppEntity implements IAzureResourceEntity {
    private final SpringCloudClusterEntity cluster;
    private final String name;
    @Nullable
    @JsonIgnore
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private transient SpringApp remote;

    public SpringCloudAppEntity(@Nonnull final String name, @Nonnull final SpringCloudClusterEntity cluster) {
        this.name = name;
        this.cluster = cluster;
    }

    SpringCloudAppEntity(@Nonnull final SpringApp resource, @Nonnull final SpringCloudClusterEntity cluster) {
        this.remote = resource;
        this.name = resource.name();
        this.cluster = cluster;
    }

    public boolean isPublic() {
        return Optional.ofNullable(this.remote).map(SpringApp::isPublic).orElse(false);
    }

    @Nullable
    public String getApplicationUrl() {
        final String url = Optional.ofNullable(this.remote).map(SpringApp::url).orElse(null);
        return StringUtils.isBlank(url) || url.equalsIgnoreCase("None") ? null : url;
    }

    public String getTestUrl() {
        return Optional.ofNullable(this.remote).map(SpringApp::activeDeploymentName).map(d -> {
            final String key = this.cluster.getRemote().listTestKeys().primaryTestEndpoint();
            return String.format("%s/%s/%s", key, this.name, d);
        }).orElse(null);
    }

    public String getLogStreamingEndpoint(String instanceName) {
        final String endpoint = this.cluster.getTestEndpoint();
        return String.format("%s/api/logstream/apps/%s/instances/%s", endpoint.replace(".test", ""), this.name, instanceName);
    }

    @Override
    public String getId() {
        return Optional.ofNullable(this.remote).map(ExternalChildResource::id)
            .orElse(this.cluster.getId() + "/apps/" + this.name);
    }

    public SpringCloudPersistentDisk getPersistentDisk() {
        final PersistentDisk disk = Optional.ofNullable(this.remote).map(SpringApp::persistentDisk).orElse(null);
        return Optional.ofNullable(disk).filter(d -> d.sizeInGB() > 0)
            .map(d -> SpringCloudPersistentDisk.builder()
                .sizeInGB(disk.sizeInGB())
                .mountPath(disk.mountPath())
                .usedInGB(disk.usedInGB()).build())
            .orElse(null);
    }

    @Override
    public String getSubscriptionId() {
        return cluster.getSubscriptionId();
    }
}
