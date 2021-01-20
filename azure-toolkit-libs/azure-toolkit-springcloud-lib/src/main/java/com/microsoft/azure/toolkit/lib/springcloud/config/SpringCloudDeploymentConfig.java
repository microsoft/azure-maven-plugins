/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudRuntimeVersion;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class SpringCloudDeploymentConfig {
    private final Integer cpu;
    private final Integer memoryInGB;
    private final Integer instanceCount;
    private final String deploymentName;
    private final String jvmOptions;
    private final String runtimeVersion;
    private final Boolean enablePersistentStorage;
    private final Map<String, String> environment;
    private final List<File> artifacts;

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

    public SpringCloudRuntimeVersion getJavaVersion() {
        return AzureSpringCloudConfigUtils.parse(runtimeVersion);
    }
}
