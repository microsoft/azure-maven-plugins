/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudRuntimeVersion;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class DemoMojo {

    private static final int GET_STATUS_TIMEOUT = 180;

    protected void doExecute() throws MojoExecutionException, AzureExecutionException {
        // Init spring clients, and prompt users to confirm
        final SpringCloudAppConfig appConfig = null;
        final File artifact = MavenArtifactUtils.getArtifactFromTargetFolder(null);
        final boolean enableDisk = appConfig.getDeployment() != null && appConfig.getDeployment().isEnablePersistentStorage();
        final String clusterName = appConfig.getClusterName();
        final String appName = appConfig.getAppName();

        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final Map<String, String> env = deploymentConfig.getEnvironment();
        final String jvmOptions = deploymentConfig.getJvmOptions();
        final ScaleSettings scaleSettings = deploymentConfig.getScaleSettings();
        final SpringCloudRuntimeVersion runtimeVersion = deploymentConfig.getJavaVersion();
        final String deploymentName = Optional.ofNullable(deploymentConfig.getDeploymentName()).orElse("default");

        final AppPlatformManager client = null;
        final AzureSpringCloud az = AzureSpringCloud.az(client);
        final SpringCloudCluster cluster = az.cluster(clusterName);
        final SpringCloudApp app = cluster.app(appName);
        final SpringCloudDeployment deployment = app.deployment(deploymentName);

        if (!app.exists()) {
            app.create().commit();
        }

        final String artifactPath = app.uploadArtifact(artifact.getPath());

        (!deployment.exists() ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configAppArtifactPath(artifactPath)
            .commit();

        app.update()
            .activate(Optional.ofNullable(appConfig.getActiveDeploymentName()).orElse(deploymentName))
            .setPublic(appConfig.isPublic())
            .enablePersistentDisk(enableDisk)
            .commit();

        deployment.waitUntilReady(GET_STATUS_TIMEOUT);
    }
}
