/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import lombok.Data;

import javax.annotation.Nonnull;

/**
 * The string version of <class>Deployment</class> which is used in `config` goal due to the reason that users may use
 * an expression(string) to represent a cpu(integer).
 */
@Data
public class AppDeploymentRawConfig {
    private String cpu;
    private String memoryInGB;
    private String instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;

    public void saveSpringCloudDeployment(@Nonnull final SpringCloudDeployment activeDeployment) {
        this.setCpu(String.valueOf(activeDeployment.getCpu()));
        this.setMemoryInGB(String.valueOf(activeDeployment.getMemoryInGB()));
        this.setInstanceCount(String.valueOf(activeDeployment.getInstances().size()));
        this.setDeploymentName(activeDeployment.getName());
        this.setJvmOptions(activeDeployment.getJvmOptions());
        this.setRuntimeVersion(activeDeployment.getRuntimeVersion());
    }
}
