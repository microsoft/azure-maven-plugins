/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentInstanceEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@EqualsAndHashCode
public class SpringCloudDeploymentConfig {
    private static final String DEFAULT_RUNTIME_VERSION = SpringCloudJavaVersion.JAVA_8;
    private static final String RUNTIME_VERSION_PATTERN = "[Jj]ava((\\s)?|_)(8|11)$";

    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
    private Map<String, String> environment;
    @Nullable
    private IArtifact artifact;

    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }

    public ScaleSettings getScaleSettings() {
        return ScaleSettings.builder()
                .capacity(instanceCount)
                .cpu(cpu)
                .memoryInGB(memoryInGB)
                .build();
    }

    public String getJavaVersion() {
        return normalize(runtimeVersion);
    }

    public static String normalize(String runtimeVersion) {
        if (StringUtils.isEmpty(runtimeVersion)) {
            return DEFAULT_RUNTIME_VERSION;
        }
        final String fixedRuntimeVersion = StringUtils.trim(runtimeVersion);
        final Matcher matcher = Pattern.compile(RUNTIME_VERSION_PATTERN).matcher(fixedRuntimeVersion);
        if (matcher.matches()) {
            return Objects.equals(matcher.group(3), "8") ? SpringCloudJavaVersion.JAVA_8 : SpringCloudJavaVersion.JAVA_11;
        } else {
            log.warn("{} is not a valid runtime version, supported values are Java 8 and Java 11, using Java 8 in this deployment.", fixedRuntimeVersion);
            return DEFAULT_RUNTIME_VERSION;
        }
    }

    public static SpringCloudDeploymentConfig fromDeployment(@Nonnull SpringCloudDeploymentEntity deploymentEntity) { // get config from deployment
        final List<SpringCloudDeploymentInstanceEntity> instances = deploymentEntity.getInstances();
        final SpringCloudPersistentDisk disk = deploymentEntity.getApp().getPersistentDisk();
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().build();
        deploymentConfig.setRuntimeVersion(deploymentEntity.getRuntimeVersion());
        deploymentConfig.setEnablePersistentStorage(Objects.nonNull(disk) && disk.getSizeInGB() > 0);
        deploymentConfig.setCpu(deploymentEntity.getCpu());
        deploymentConfig.setMemoryInGB(deploymentEntity.getMemoryInGB());
        deploymentConfig.setInstanceCount(instances.size());
        deploymentConfig.setJvmOptions(Optional.ofNullable(deploymentEntity.getJvmOptions()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(deploymentEntity.getEnvironmentVariables()).orElse(new HashMap<>()));
        return deploymentConfig;
    }

}
