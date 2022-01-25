/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @deprecated use {@link com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft} instead.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
@EqualsAndHashCode
@Deprecated
public class SpringCloudDeploymentConfig {
    private static final String DEFAULT_RUNTIME_VERSION = SpringCloudJavaVersion.JAVA_8;
    private static final String RUNTIME_VERSION_PATTERN = "[Jj]ava((\\s)?|_)(8|11)$";

    @Builder.Default
    private Integer cpu = 1;
    @Builder.Default
    private Integer memoryInGB = 1;
    private Integer instanceCount;
    private String deploymentName;
    @Nullable
    private String jvmOptions;
    @Builder.Default
    private String runtimeVersion = RuntimeVersion.JAVA_8.toString();
    @Builder.Default
    private Boolean enablePersistentStorage = false;
    @Nullable
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

    @Nullable
    @Contract("null -> null")
    public static SpringCloudDeploymentConfig fromDeployment(@Nullable SpringCloudDeployment deployment) { // get config from deployment
        if (Objects.isNull(deployment)) {
            return null;
        }
        final List<DeploymentInstance> instances = deployment.getInstances();
        final SpringCloudPersistentDisk disk = deployment.getParent().getPersistentDisk();
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().build();
        deploymentConfig.setRuntimeVersion(deployment.getRuntimeVersion());
        deploymentConfig.setEnablePersistentStorage(Objects.nonNull(disk) && disk.getSizeInGB() > 0);
        deploymentConfig.setCpu(deployment.getCpu());
        deploymentConfig.setMemoryInGB(deployment.getMemoryInGB());
        deploymentConfig.setInstanceCount(instances.size());
        deploymentConfig.setJvmOptions(Optional.ofNullable(deployment.getJvmOptions()).map(String::trim).orElse(""));
        deploymentConfig.setEnvironment(Optional.ofNullable(deployment.getEnvironmentVariables()).orElse(new HashMap<>()));
        return deploymentConfig;
    }

}
