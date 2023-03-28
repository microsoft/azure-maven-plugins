/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.task;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.springcloud.*;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;

@Getter
public class DeploySpringCloudAppTask extends AzureTask<SpringCloudDeployment> {
    public static final String DEFAULT_DEPLOYMENT_NAME = "default";

    private final SpringCloudAppConfig config;
    @Nonnull
    private final List<AzureTask<?>> subTasks;
    private SpringCloudDeployment deployment;
    private final boolean openStreamingLogOnFailure;
    private final boolean waitDeploymentComplete;
    private static final int TIMEOUT_IN_SECONDS = 60;
    private static final String GET_APP_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
            "you can check the app status from Azure Portal.";
    private static final String START_APP = "Starting Web App after deploying artifacts...";

    public DeploySpringCloudAppTask(SpringCloudAppConfig appConfig) {
        this(appConfig, false, false);
    }

    public DeploySpringCloudAppTask(SpringCloudAppConfig appConfig, boolean openStreamingLogOnFailure, boolean waitDeploymentComplete) {
        this.config = appConfig;
        this.subTasks = this.initTasks();
        this.openStreamingLogOnFailure = openStreamingLogOnFailure;
        this.waitDeploymentComplete = waitDeploymentComplete;
    }

    @Nonnull
    private List<AzureTask<?>> initTasks() {
        // Init spring clients, and prompt users to confirm
        final SpringCloudDeploymentConfig deploymentConfig = config.getDeployment();
        final String clusterName = config.getClusterName();
        final String appName = config.getAppName();
        final String resourceGroup = config.getResourceGroup();
        final SpringCloudCluster cluster = Azure.az(AzureSpringCloud.class).clusters(config.getSubscriptionId()).get(clusterName, resourceGroup);
        Optional.ofNullable(cluster).orElseThrow(() -> new AzureToolkitRuntimeException(
            String.format("Azure Spring Apps(%s) is not found in subscription(%s).", clusterName, config.getSubscriptionId())));
        final SpringCloudAppDraft app = cluster.apps().updateOrCreate(appName, resourceGroup);
        final String deploymentName = StringUtils.firstNonBlank(
            deploymentConfig.getDeploymentName(),
            config.getActiveDeploymentName(),
            app.getActiveDeploymentName(),
            DEFAULT_DEPLOYMENT_NAME
        );
        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = toCreateApp && !app.deployments().exists(deploymentName, resourceGroup);
        config.setActiveDeploymentName(StringUtils.firstNonBlank(app.getActiveDeploymentName(), toCreateDeployment ? deploymentName : null));

        OperationContext.action().setTelemetryProperty("subscriptionId", config.getSubscriptionId());
        OperationContext.current().setTelemetryProperty("isCreateNewApp", String.valueOf(toCreateApp));
        OperationContext.current().setTelemetryProperty("isCreateDeployment", String.valueOf(toCreateDeployment));
        OperationContext.current().setTelemetryProperty("isDeploymentNameGiven", String.valueOf(StringUtils.isNotEmpty(deploymentConfig.getDeploymentName())));

        final AzureString CREATE_APP_TITLE = AzureString.format("Create new app({0}) in Azure Spring Apps({1})", appName, clusterName);
        final AzureString UPDATE_APP_TITLE = AzureString.format("Update app({0}) of Azure Spring Apps({1})", appName, clusterName);
        final AzureString CREATE_DEPLOYMENT_TITLE = AzureString.format("Create new deployment({0}) in app({1})", deploymentName, appName);
        final AzureString UPDATE_DEPLOYMENT_TITLE = AzureString.format("Update deployment({0}) of app({1})", deploymentName, appName);
        final AzureString MODIFY_DEPLOYMENT_TITLE = toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE;

        final List<AzureTask<?>> tasks = new ArrayList<>();
        app.setConfig(config);
        if (toCreateApp) {
            tasks.add(new AzureTask<Void>(CREATE_APP_TITLE, app::createIfNotExist));
        }
        tasks.add(new AzureTask<Void>(MODIFY_DEPLOYMENT_TITLE, () -> {
            final SpringCloudDeploymentDraft draft = app.deployments().updateOrCreate(deploymentName, resourceGroup);
            draft.setConfig(config.getDeployment());
            try {
                this.deployment = draft.commit();
            } catch (final Exception e) {
                app.refresh();
                this.deployment = app.getActiveDeployment();
                startStreamingLog(true);
                throw new AzureToolkitRuntimeException(e);
            }
        }));
        tasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, () -> {
            final SpringCloudAppDraft draft = (SpringCloudAppDraft) app.update();
            draft.setConfig(config);
            draft.updateIfExist();
        }));
        tasks.add(new AzureTask<Void>(app::reset));
        if (this.waitDeploymentComplete) {
            tasks.add(new AzureTask<Void>(this::startApp));
        }
        return tasks;
    }

    @Override
    @AzureOperation(name = "internal/springcloud.create_update_app.app", params = {"this.config.getAppName()"})
    public SpringCloudDeployment doExecute() throws Exception {
        for (final AzureTask<?> t : this.subTasks) {
            t.getBody().call();
        }
        return this.deployment;
    }

    private void startStreamingLog(boolean follow) {
        if (Objects.isNull(this.deployment) || !openStreamingLogOnFailure) {
            return;
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        Optional.ofNullable(this.deployment.getLatestInstance()).ifPresent(i -> {
            messager.info(AzureString.format("Opening streaming log of instance({0})...", i.getName()));
            messager.debug("###############STREAMING LOG BEGIN##################");
            // refer to https://github.com/Azure/azure-cli-extensions/blob/main/src/spring/azext_spring/app.py#app_tail_log_internal
            this.deployment.streamLogs(i.getName(), 300, 500, 1024 * 1024, follow)
                    .doFinally(type -> messager.debug("###############STREAMING LOG END##################"))
                    .subscribe(messager::debug);
        });
    }

    private void startApp() {
        AzureMessager.getMessager().info(START_APP);
        if (!deployment.waitUntilReady(TIMEOUT_IN_SECONDS)) {
            AzureMessager.getMessager().warning(GET_APP_STATUS_TIMEOUT);
            startStreamingLog(false);
        }
    }
}
