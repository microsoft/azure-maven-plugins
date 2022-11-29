/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @deprecated use {@link com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft} instead.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@EqualsAndHashCode
@Deprecated
public class SpringCloudDeploymentConfig {

    @Nullable
    @Builder.Default
    private Double cpu = 1.0;
    @Nullable
    @Builder.Default
    private Double memoryInGB = 1.0;
    private Integer instanceCount;
    private String deploymentName;
    @Nullable
    private String jvmOptions;
    @Builder.Default
    private String runtimeVersion = SpringCloudDeploymentDraft.DEFAULT_RUNTIME_VERSION.toString();
    @Nonnull
    @Builder.Default
    private Boolean enablePersistentStorage = false;
    @Nullable
    private Map<String, String> environment;
    @Nullable
    private IArtifact artifact;

    @Nonnull
    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }

    @Nullable
    public String getJavaVersion() {
        return SpringCloudDeploymentDraft.formalizeRuntimeVersion(runtimeVersion).toString();
    }

    @Nullable
    @Contract("null -> null")
    public static SpringCloudDeploymentConfig fromDeployment(@Nullable SpringCloudDeployment deployment) { // get config from deployment
        if (Objects.isNull(deployment)) {
            return null;
        }
        final SpringCloudPersistentDisk disk = deployment.getParent().getPersistentDisk();
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().build();
        deploymentConfig.setRuntimeVersion(deployment.getRuntimeVersion());
        deploymentConfig.setEnablePersistentStorage(Objects.nonNull(disk) && disk.getSizeInGB() > 0);
        deploymentConfig.setCpu(deployment.getCpu());
        deploymentConfig.setMemoryInGB(deployment.getMemoryInGB());
        deploymentConfig.setInstanceCount(deployment.getInstanceNum());
        deploymentConfig.setJvmOptions(Optional.ofNullable(deployment.getJvmOptions()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(deployment.getEnvironmentVariables()).orElse(new HashMap<>()));
        return deploymentConfig;
    }

}
