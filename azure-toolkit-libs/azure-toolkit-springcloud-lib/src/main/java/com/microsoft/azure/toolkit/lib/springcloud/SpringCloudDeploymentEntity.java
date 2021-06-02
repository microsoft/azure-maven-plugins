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

import com.azure.resourcemanager.appplatform.models.DeploymentResourceStatus;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.resources.fluentcore.arm.models.ExternalChildResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentStatus;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class SpringCloudDeploymentEntity implements IAzureResourceEntity {
    @Nonnull
    private final SpringCloudAppEntity app;
    @Nonnull
    private final String name;
    @Nullable
    @JsonIgnore
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private transient SpringAppDeployment remote;

    public SpringCloudDeploymentEntity(@Nonnull final String name, @Nonnull SpringCloudAppEntity app) {
        this.name = name;
        this.app = app;
    }

    SpringCloudDeploymentEntity(@Nonnull SpringAppDeployment remote, @Nonnull SpringCloudAppEntity app) {
        this.remote = remote;
        this.name = remote.name();
        this.app = app;
    }

    public Integer getCpu() {
        return Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::cpu)
            .orElse(1);
    }

    public Integer getMemoryInGB() {
        return Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::memoryInGB)
            .orElse(1);
    }

    public SpringCloudDeploymentStatus getStatus() {
        final String status = Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::status)
            .orElse(DeploymentResourceStatus.UNKNOWN).toString();
        return SpringCloudDeploymentStatus.valueOf(status.toUpperCase());
    }

    public String getRuntimeVersion() {
        return Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::settings)
            .map(s -> s.runtimeVersion().toString())
            .orElse(SpringCloudJavaVersion.JAVA_8);
    }

    public String getJvmOptions() {
        return Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::jvmOptions)
            .orElse(null);
    }

    public Map<String, String> getEnvironmentVariables() {
        return Optional.ofNullable(this.remote)
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::environmentVariables)
            .orElse(null);
    }

    public List<SpringCloudDeploymentInstanceEntity> getInstances() {
        if (Objects.nonNull(this.remote)) {
            return this.remote.instances().stream()
                .map(i -> new SpringCloudDeploymentInstanceEntity(i, this))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public Boolean isActive() {
        return Optional.ofNullable(this.remote).map(SpringAppDeployment::isActive).orElse(false);
    }

    @Override
    public String getId() {
        return Optional.ofNullable(this.remote).map(ExternalChildResource::id)
                .orElse(this.app.getId() + "/deployments/" + this.name);
    }

    @Override
    public String getSubscriptionId() {
        return app.getSubscriptionId();
    }
}
