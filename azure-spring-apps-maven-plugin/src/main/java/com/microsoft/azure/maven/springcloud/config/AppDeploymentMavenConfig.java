/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.model.Resource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Getter
public class AppDeploymentMavenConfig {
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
    @Nullable
    private Map<String, String> environment;
    @Nullable
    private List<Resource> resources;

    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }

    public AppDeploymentMavenConfig withCpu(Integer cpu) {
        this.cpu = cpu;
        return this;
    }

    public AppDeploymentMavenConfig withMemoryInGB(Integer memoryInGB) {
        this.memoryInGB = memoryInGB;
        return this;
    }

    public AppDeploymentMavenConfig withInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
        return this;
    }

    public AppDeploymentMavenConfig withJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
        return this;
    }

    public AppDeploymentMavenConfig withEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    public AppDeploymentMavenConfig withDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }

    public AppDeploymentMavenConfig withResources(List<Resource> resources) {
        this.resources = resources;
        return this;
    }

    public AppDeploymentMavenConfig withEnablePersistentStorage(Boolean enablePersistentStorage) {
        this.enablePersistentStorage = enablePersistentStorage;
        return this;
    }

    public AppDeploymentMavenConfig withRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }
}
