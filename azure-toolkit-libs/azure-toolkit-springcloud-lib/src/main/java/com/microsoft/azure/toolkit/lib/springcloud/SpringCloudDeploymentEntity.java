/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.DeploymentResourceStatus;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.resources.fluentcore.arm.models.ExternalChildResource;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource.RemoteAwareResourceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentStatus;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class SpringCloudDeploymentEntity extends RemoteAwareResourceEntity<SpringAppDeployment> {
    @Nonnull
    private final SpringCloudAppEntity app;
    @Nonnull
    private final String name;

    public SpringCloudDeploymentEntity(@Nonnull final String name, @Nonnull SpringCloudAppEntity app) {
        this.name = name;
        this.app = app;
    }

    SpringCloudDeploymentEntity(@Nonnull SpringAppDeployment remote, @Nonnull SpringCloudAppEntity app) {
        this.remote = remote;
        this.name = remote.name();
        this.app = app;
    }

    @Nonnull
    public Integer getCpu() {
        return Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::settings)
                .map(DeploymentSettings::cpu)
                .orElse(1);
    }

    @Nonnull
    public Integer getMemoryInGB() {
        return Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::settings)
                .map(DeploymentSettings::memoryInGB)
                .orElse(1);
    }

    @Nonnull
    public SpringCloudDeploymentStatus getStatus() {
        final String status = Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::status)
                .orElse(DeploymentResourceStatus.UNKNOWN).toString();
        return SpringCloudDeploymentStatus.valueOf(status.toUpperCase());
    }

    @Nonnull
    public String getRuntimeVersion() {
        return Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::settings)
                .map(s -> s.runtimeVersion().toString())
                .orElse(SpringCloudJavaVersion.JAVA_8);
    }

    @Nullable
    public String getJvmOptions() {
        return Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::settings)
                .map(DeploymentSettings::jvmOptions)
                .orElse(null);
    }

    @Nullable
    public Map<String, String> getEnvironmentVariables() {
        return Optional.ofNullable(this.remote)
                .map(SpringAppDeployment::settings)
                .map(DeploymentSettings::environmentVariables)
                .orElse(null);
    }

    @Nonnull
    public List<SpringCloudDeploymentInstanceEntity> getInstances() {
        if (Objects.nonNull(this.remote)) {
            return this.remote.instances().stream()
                    .map(i -> new SpringCloudDeploymentInstanceEntity(i, this))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Nonnull
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
