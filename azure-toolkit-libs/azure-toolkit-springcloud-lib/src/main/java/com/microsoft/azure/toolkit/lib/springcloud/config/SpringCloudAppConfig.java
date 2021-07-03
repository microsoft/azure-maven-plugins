/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SpringCloudAppConfig {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String resourceGroup;
    private Boolean isPublic;
    private String runtimeVersion;
    private String activeDeploymentName;
    private SpringCloudDeploymentConfig deployment;

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }

    public static SpringCloudAppConfig fromApp(@Nonnull SpringCloudApp app) { // get config from app
        final SpringCloudAppEntity appEntity = app.entity();
        final SpringCloudPersistentDisk disk = appEntity.getPersistentDisk();
        final SpringCloudDeploymentEntity deploymentEntity = Optional.ofNullable(app.activeDeployment())
                .map(SpringCloudDeployment::entity)
                .orElse(new SpringCloudDeploymentEntity("default", app.entity()));
        final List<SpringCloudDeploymentInstanceEntity> instances = deploymentEntity.getInstances();

        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().build();
        final SpringCloudAppConfig appConfig = SpringCloudAppConfig.builder().deployment(deploymentConfig).build();
        appConfig.setSubscriptionId(app.subscriptionId());
        appConfig.setClusterName(app.getCluster().name());
        appConfig.setAppName(app.name());
        appConfig.setIsPublic(Objects.equals(app.entity().isPublic(), true));
        deploymentConfig.setRuntimeVersion(deploymentEntity.getRuntimeVersion());
        deploymentConfig.setEnablePersistentStorage(Objects.nonNull(disk) && disk.getSizeInGB() > 0);
        deploymentConfig.setCpu(deploymentEntity.getCpu());
        deploymentConfig.setMemoryInGB(deploymentEntity.getMemoryInGB());
        deploymentConfig.setInstanceCount(instances.size());
        deploymentConfig.setJvmOptions(Optional.ofNullable(deploymentEntity.getJvmOptions()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(deploymentEntity.getEnvironmentVariables()).orElse(new HashMap<>()));
        return appConfig;
    }
}
