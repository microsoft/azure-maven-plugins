/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import org.apache.maven.model.Resource;

import java.util.List;
import java.util.Map;

public class Deployment {
    private int cpu;
    private int memoryInGB;
    private int instanceCount;
    private String jvmParameter;
    private Map<String, String> environment;
    private List<Volume> volumes;
    private List<Resource> resources;

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getMemoryInGB() {
        return memoryInGB;
    }

    public void setMemoryInGB(int memoryInGB) {
        this.memoryInGB = memoryInGB;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public String getJvmParameter() {
        return jvmParameter;
    }

    public void setJvmParameter(String jvmParameter) {
        this.jvmParameter = jvmParameter;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public boolean validate() throws SpringConfigurationException {
        return true;
    }

    public Volume getPersistentDisk() {
        return volumes.stream().filter(Volume::isPersist).findFirst().orElse(null);
    }

    public Volume getTemporaryDisk() {
        return volumes.stream().filter(volume -> !volume.isPersist()).findFirst().orElse(null);
    }
}
