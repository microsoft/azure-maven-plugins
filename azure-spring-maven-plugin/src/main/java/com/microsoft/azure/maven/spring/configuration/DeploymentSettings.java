/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

import com.microsoft.azure.maven.spring.utils.XmlUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * The string version of <class>Deployment</class> which is used in `config` goal due to the reason that users may use
 * an expression(string) to represent a cpu(integer).
 */
public class DeploymentSettings {
    private String cpu;
    private String memoryInGB;
    private String instanceCount;
    private String deploymentName;
    private String jvmOptions;

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

    public void applyToXpp3Dom(Xpp3Dom deployment) {
        XmlUtils.replaceDomWithKeyValue(deployment, "cpu", this.cpu);
        XmlUtils.replaceDomWithKeyValue(deployment, "memoryInGB", this.memoryInGB);
        XmlUtils.replaceDomWithKeyValue(deployment, "instanceCount", this.instanceCount);
        XmlUtils.replaceDomWithKeyValue(deployment, "jvmOptions", this.jvmOptions);
    }
}
