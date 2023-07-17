/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.springcloud.AbstractMojoBase;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudClusterConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigurationParser {
    public SpringCloudAppConfig parse(AbstractMojoBase springMojo) {
        final AppDeploymentMavenConfig rawConfig = springMojo.getDeployment();
        final SpringCloudDeploymentConfig config = ConfigurationParser.toDeploymentConfig(rawConfig, springMojo);
        final SpringCloudClusterConfig clusterConfig = ConfigurationParser.toClusterConfig(springMojo);
        return SpringCloudAppConfig.builder()
            .appName(springMojo.getAppName())
            .cluster(clusterConfig)
            .deployment(config)
            .isPublic(springMojo.getIsPublic())
            .build();
    }

    private static SpringCloudClusterConfig toClusterConfig(@Nonnull final AbstractMojoBase springMojo) {
        return SpringCloudClusterConfig.builder()
            .clusterName(springMojo.getClusterName())
            .resourceGroup(springMojo.getResourceGroup())
            .region(springMojo.getRegion())
            .subscriptionId(springMojo.getSubscriptionId())
            .sku(springMojo.getSku())
//            .environment(springMojo.getEnvironment())
//            .environmentResourceGroup(StringUtils.firstNonBlank(springMojo.getEnvironmentResourceGroup(), springMojo.getResourceGroup()))
            .build();
    }

    @SneakyThrows
    private static SpringCloudDeploymentConfig toDeploymentConfig(AppDeploymentMavenConfig rawConfig, AbstractMojoBase mojo) {
        final List<File> artifacts = new ArrayList<>();
        Optional.ofNullable(rawConfig.getResources()).ifPresent(resources -> resources.forEach(resource -> {
            try {
                artifacts.addAll(MavenArtifactUtils.getArtifacts(resource));
            } catch (IllegalStateException e) {
                AzureMessager.getMessager().warning(String.format("'%s' doesn't exist or isn't a directory", resource.getDirectory()));
            }
        }));
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
            .capacity(rawConfig.getInstanceCount())
            .jvmOptions(rawConfig.getJvmOptions())
            .memoryInGB(rawConfig.getMemoryInGB())
            .runtimeVersion(StringUtils.firstNonEmpty(rawConfig.getRuntimeVersion(), mojo.getRuntimeVersion()))
            .build();
    }

    public static ConfigurationParser getInstance() {
        return Holder.parser;
    }

    private static class Holder {
        private static final ConfigurationParser parser = new ConfigurationParser();
    }
}
