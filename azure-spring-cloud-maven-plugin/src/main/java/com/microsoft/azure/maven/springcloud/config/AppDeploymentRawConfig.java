/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import lombok.Data;

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
}
