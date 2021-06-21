/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class DeploySpringCloudAppTask extends AzureTask<SpringCloudDeployment> {
    public static final String DEFAULT_DEPLOYMENT_NAME = "default";

    private final SpringCloudAppConfig config;
    private final List<AzureTask<?>> subTasks;
    private SpringCloudDeployment deployment;

    public DeploySpringCloudAppTask(SpringCloudAppConfig appConfig) {
        this.config = appConfig;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        final IAzureMessager messager = AzureMessager.getMessager();
        // Init spring clients, and prompt users to confirm
        final SpringCloudDeploymentConfig deploymentConfig = config.getDeployment();
        final File file = Optional.ofNullable(deploymentConfig.getArtifact()).map(IArtifact::getFile).orElse(null);
        final boolean enableDisk = config.getDeployment() != null && config.getDeployment().isEnablePersistentStorage();
        final Map<String, String> env = deploymentConfig.getEnvironment();
        final String jvmOptions = deploymentConfig.getJvmOptions();
        final ScaleSettings scaleSettings = deploymentConfig.getScaleSettings();
        final String runtimeVersion = deploymentConfig.getJavaVersion();

        final String clusterName = config.getClusterName();
        final String appName = config.getAppName();
        final SpringCloudCluster cluster = Azure.az(AzureSpringCloud.class).subscription(config.getSubscriptionId()).cluster(clusterName);
        Optional.ofNullable(cluster).orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Service(%s) is not found", clusterName)));
        final SpringCloudApp app = cluster.app(appName);
        final String deploymentName = StringUtils.firstNonBlank(
                deploymentConfig.getDeploymentName(),
                config.getActiveDeploymentName(),
                app.activeDeploymentName(),
                DEFAULT_DEPLOYMENT_NAME
        );
        this.deployment = app.deployment(deploymentName);

        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !deployment.exists();

        AzureTelemetry.getContext().getActionProperties().put("subscriptionId", config.getSubscriptionId());
        AzureTelemetry.getContext().setProperty("isCreateNewApp", String.valueOf(toCreateApp));
        AzureTelemetry.getContext().setProperty("isCreateDeployment", String.valueOf(toCreateDeployment));
        AzureTelemetry.getContext().setProperty("isDeploymentNameGiven", String.valueOf(StringUtils.isNotEmpty(deploymentConfig.getDeploymentName())));

        final String CREATE_APP_TITLE = String.format("Create new app(%s) on service(%s)", messager.value(appName), messager.value(clusterName));
        final String UPDATE_APP_TITLE = String.format("Update app(%s) of service(%s)", messager.value(appName), messager.value(clusterName));
        final String CREATE_DEPLOYMENT_TITLE = String.format("Create new deployment(%s) in app(%s)", messager.value(deploymentName), messager.value(appName));
        final String UPDATE_DEPLOYMENT_TITLE = String.format("Update deployment(%s) of app(%s)", messager.value(deploymentName), messager.value(appName));
        final String DEPLOYMENT_TITLE = toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE;

        final List<AzureTask<?>> tasks = new ArrayList<>();
        if (toCreateApp) {
            tasks.add(new AzureTask<Void>(CREATE_APP_TITLE, () -> app.create().commit()));
        }
        tasks.add(new AzureTask<Void>(DEPLOYMENT_TITLE, () -> (toCreateDeployment ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configArtifact(file)
            .commit()));
        tasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, () -> app.update()
            // active deployment should keep active.
            .activate(StringUtils.firstNonBlank(app.activeDeploymentName(), toCreateDeployment ? deploymentName : null))
            .setPublic(config.isPublic())
            .enablePersistentDisk(enableDisk).commit()));
        return tasks;
    }

    @Override
    @AzureOperation(name = "springcloud|app.create_update", params = {"this.config.getAppName()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment execute() {
        this.subTasks.forEach(t->t.getSupplier().get());
        return this.deployment;
    }
}
