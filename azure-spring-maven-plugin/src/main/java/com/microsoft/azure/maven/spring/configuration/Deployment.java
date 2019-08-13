/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.XmlUtils;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.List;
import java.util.Map;

public class Deployment {
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String jvmOptions;
    private Map<String, String> environment;
    private List<Volume> volumes;
    private List<Resource> resources;

    public Integer getCpu() {
        return cpu;
    }

    public Integer getMemoryInGB() {
        return memoryInGB;
    }

    public Integer getInstanceCount() {
        return instanceCount;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public Volume getPersistentDisk() {
        return volumes == null ? null : volumes.stream().filter(Volume::isPersist).findFirst().orElse(null);
    }

    public Volume getTemporaryDisk() {
        return volumes == null ? null : volumes.stream().filter(volume -> !volume.isPersist()).findFirst().orElse(null);
    }

    public Deployment withCpu(Integer cpu) {
        this.cpu = cpu;
        return this;
    }

    public Deployment withMemoryInGB(Integer memoryInGB) {
        this.memoryInGB = memoryInGB;
        return this;
    }

    public Deployment withInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
        return this;
    }

    public Deployment withJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
        return this;
    }

    public Deployment withEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    public Deployment withVolumes(List<Volume> volumes) {
        this.volumes = volumes;
        return this;
    }

    public Deployment withResources(List<Resource> resources) {
        this.resources = resources;
        return this;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void applyToXpp3Dom(Xpp3Dom deployment) {
        XmlUtils.replaceDomWithKeyValue(deployment, "cpu", this.cpu);
        XmlUtils.replaceDomWithKeyValue(deployment, "memoryInGB", this.memoryInGB);
        XmlUtils.replaceDomWithKeyValue(deployment, "instanceCount", this.instanceCount);
        XmlUtils.replaceDomWithKeyValue(deployment, "jvmOptions", this.jvmOptions);
    }
}
