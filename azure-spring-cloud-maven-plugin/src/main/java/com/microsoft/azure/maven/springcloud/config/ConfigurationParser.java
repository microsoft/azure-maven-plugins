/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.springcloud.AbstractMojoBase;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.tools.springcloud.AppConfig;
import com.microsoft.azure.tools.springcloud.AppDeploymentConfig;

public class ConfigurationParser {
    public AppConfig parse(AbstractMojoBase springMojo) {
        final AppDeploymentMavenConfig rawConfig = springMojo.getDeployment();
        final AppDeploymentConfig config = ConfigurationParser.toDeploymentConfig(rawConfig);
        return new AppConfig()
            .withAppName(springMojo.getAppName())
            .withClusterName(springMojo.getClusterName())
            .withDeployment(config)
            .withRuntimeVersion(springMojo.getRuntimeVersion())
            .withPublic(springMojo.isPublic())
            .withSubscriptionId(springMojo.getSubscriptionId());
    }

    private static AppDeploymentConfig toDeploymentConfig(AppDeploymentMavenConfig rawConfig) {
        return new AppDeploymentConfig()
            .withCpu(rawConfig.getCpu())
            .withDeploymentName(rawConfig.getDeploymentName())
            .withArtifacts(MavenArtifactUtils.getArtifacts(rawConfig.getResources()))
            .withEnablePersistentStorage(rawConfig.isEnablePersistentStorage())
            .withEnvironment(rawConfig.getEnvironment())
            .withInstanceCount(rawConfig.getInstanceCount())
            .withJvmOptions(rawConfig.getJvmOptions())
            .withMemoryInGB(rawConfig.getMemoryInGB())
            .withRuntimeVersion(rawConfig.getRuntimeVersion());
    }

    public static ConfigurationParser getInstance() {
        return Holder.parser;
    }

    private static class Holder {
        private static final ConfigurationParser parser = new ConfigurationParser();
    }
}
