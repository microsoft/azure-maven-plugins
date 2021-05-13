/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.springcloud.AbstractMojoBase;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.SneakyThrows;

import java.io.File;
import java.util.List;

public class ConfigurationParser {
    public SpringCloudAppConfig parse(AbstractMojoBase springMojo) {
        final AppDeploymentMavenConfig rawConfig = springMojo.getDeployment();
        final SpringCloudDeploymentConfig config = ConfigurationParser.toDeploymentConfig(rawConfig, springMojo);
        return SpringCloudAppConfig.builder()
            .appName(springMojo.getAppName())
            .clusterName(springMojo.getClusterName())
            .deployment(config)
            .runtimeVersion(springMojo.getRuntimeVersion())
            .isPublic(springMojo.getIsPublic())
            .subscriptionId(springMojo.getSubscriptionId())
            .build();
    }

    @SneakyThrows
    private static SpringCloudDeploymentConfig toDeploymentConfig(AppDeploymentMavenConfig rawConfig, AbstractMojoBase mojo) {
        final List<File> artifacts = MavenArtifactUtils.getArtifacts(rawConfig.getResources());
        if (artifacts.isEmpty()) {
            artifacts.addAll(MavenArtifactUtils.getArtifactFiles(mojo.getProject()));
        }
        final File artifact = MavenArtifactUtils.getExecutableJarFiles(artifacts);
        return SpringCloudDeploymentConfig.builder()
            .cpu(rawConfig.getCpu())
            .deploymentName(rawConfig.getDeploymentName())
            .artifact(artifact != null ? IArtifact.fromFile(artifact) : null)
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
