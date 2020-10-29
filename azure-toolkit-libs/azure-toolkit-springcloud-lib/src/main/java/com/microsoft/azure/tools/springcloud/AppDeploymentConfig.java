/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.springcloud;

import org.apache.commons.lang3.BooleanUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AppDeploymentConfig {
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
    private Map<String, String> environment;
    private List<File> artifacts;

    public Integer getCpu() {
        return cpu;
    }

    public Integer getMemoryInGB() {
        return memoryInGB;
    }

    public Integer getInstanceCount() {
        return instanceCount;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<File> getArtifacts() {
        return artifacts;
    }

    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public AppDeploymentConfig withCpu(Integer cpu) {
        this.cpu = cpu;
        return this;
    }

    public AppDeploymentConfig withMemoryInGB(Integer memoryInGB) {
        this.memoryInGB = memoryInGB;
        return this;
    }

    public AppDeploymentConfig withInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
        return this;
    }

    public AppDeploymentConfig withJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
        return this;
    }

    public AppDeploymentConfig withEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    public AppDeploymentConfig withDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }

    public AppDeploymentConfig withArtifacts(List<File> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public AppDeploymentConfig withEnablePersistentStorage(Boolean enablePersistentStorage) {
        this.enablePersistentStorage = enablePersistentStorage;
        return this;
    }

    public AppDeploymentConfig withRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }
}
