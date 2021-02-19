/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.springcloud.AbstractMojoBase;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;

public class ConfigurationParser {
    public SpringCloudAppConfig parse(AbstractMojoBase springMojo) {
        final AppDeploymentMavenConfig rawConfig = springMojo.getDeployment();
        final SpringCloudDeploymentConfig config = ConfigurationParser.toDeploymentConfig(rawConfig);
        return SpringCloudAppConfig.builder()
            .appName(springMojo.getAppName())
            .clusterName(springMojo.getClusterName())
            .deployment(config)
            .runtimeVersion(springMojo.getRuntimeVersion())
            .isPublic(springMojo.isPublic())
            .subscriptionId(springMojo.getSubscriptionId())
            .build();
    }

    private static SpringCloudDeploymentConfig toDeploymentConfig(AppDeploymentMavenConfig rawConfig) {
        return SpringCloudDeploymentConfig.builder()
            .cpu(rawConfig.getCpu())
            .deploymentName(rawConfig.getDeploymentName())
            .artifacts(MavenArtifactUtils.getArtifacts(rawConfig.getResources()))
            .enablePersistentStorage(rawConfig.isEnablePersistentStorage())
            .environment(rawConfig.getEnvironment())
            .instanceCount(rawConfig.getInstanceCount())
            .jvmOptions(rawConfig.getJvmOptions())
            .memoryInGB(rawConfig.getMemoryInGB())
            .runtimeVersion(rawConfig.getRuntimeVersion())
            .build();
    }

    public static ConfigurationParser getInstance() {
        return Holder.parser;
    }

    private static class Holder {
        private static final ConfigurationParser parser = new ConfigurationParser();
    }
}
