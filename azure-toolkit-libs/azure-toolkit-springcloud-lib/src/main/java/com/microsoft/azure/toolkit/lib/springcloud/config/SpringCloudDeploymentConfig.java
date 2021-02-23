/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nullable;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpringCloudDeploymentConfig {
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
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
        return AzureSpringCloudConfigUtils.normalize(runtimeVersion);
    }
}
