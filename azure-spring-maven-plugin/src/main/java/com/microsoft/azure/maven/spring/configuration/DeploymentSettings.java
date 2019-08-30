/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The string version of <class>Deployment</class> which is used in `config` goal due to the reason that users may use
 * an expression(string) to represent a cpu(integer).
 */
public class DeploymentSettings extends BaseSettings {
    private String cpu;
    private String memoryInGB;
    private String instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;

    public String getCpu() {
        return cpu;
    }

    public String getMemoryInGB() {
        return memoryInGB;
    }

    public String getInstanceCount() {
        return instanceCount;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public DeploymentSettings withCpu(String cpu) {
        this.cpu = cpu;
        return this;
    }

    public DeploymentSettings withMemoryInGB(String memoryInGB) {
        this.memoryInGB = memoryInGB;
        return this;
    }

    public DeploymentSettings withInstanceCount(String instanceCount) {
        this.instanceCount = instanceCount;
        return this;
    }

    public DeploymentSettings withJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
        return this;
    }

    public DeploymentSettings withDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    @Override
    protected Map<String, Object> getProperties() {
        return MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[] {
            new DefaultMapEntry("cpu", this.cpu),
            new DefaultMapEntry("memoryInGB", this.memoryInGB),
            new DefaultMapEntry("instanceCount", this.instanceCount),
            new DefaultMapEntry("jvmOptions", this.jvmOptions),
            new DefaultMapEntry("runtimeVersion", this.runtimeVersion),
        });
    }
}
