/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.task;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DeploySpringCloudAppTask extends AzureTask<SpringCloudDeployment> {
    public static final String DEFAULT_DEPLOYMENT_NAME = "default";

    private final List<AzureTask<?>> subTasks;
    private final SpringCloudDeploymentDraft deployment;

    public DeploySpringCloudAppTask(SpringCloudDeploymentDraft deployment) {
        this.deployment = deployment;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        // Init spring clients, and prompt users to confirm
        final SpringCloudAppDraft app = (SpringCloudAppDraft) this.deployment.getParent();
        final SpringCloudCluster cluster = app.getParent();
        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !deployment.exists() && !(toCreateApp && DEFAULT_DEPLOYMENT_NAME.equals(deployment.getName()));
        app.setActiveDeploymentName(StringUtils.firstNonBlank(app.getActiveDeploymentName(), toCreateDeployment ? deployment.getName() : null));

        AzureTelemetry.getActionContext().setProperty("subscriptionId", deployment.getSubscriptionId());
        AzureTelemetry.getContext().setProperty("isCreateNewApp", String.valueOf(toCreateApp));
        AzureTelemetry.getContext().setProperty("isCreateDeployment", String.valueOf(toCreateDeployment));

        final AzureString CREATE_APP_TITLE = AzureString.format("Create new app({0}) on service({1})", app.getName(), cluster.getName());
        final AzureString UPDATE_APP_TITLE = AzureString.format("Update app({0}) of service({1})", app.getName(), cluster.getName());
        final AzureString CREATE_DEPLOYMENT_TITLE = AzureString.format("Create new deployment({0}) in app({1})", deployment.getName(), app.getName());
        final AzureString UPDATE_DEPLOYMENT_TITLE = AzureString.format("Update deployment({0}) of app({1})", deployment.getName(), app.getName());
        final AzureString MODIFY_DEPLOYMENT_TITLE = toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE;

        final List<AzureTask<?>> tasks = new ArrayList<>();
        if (toCreateApp) {
            tasks.add(new AzureTask<Void>(CREATE_APP_TITLE, app::createIfNotExist));
        }
        tasks.add(new AzureTask<Void>(MODIFY_DEPLOYMENT_TITLE, deployment::commit));
        tasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, app::updateIfExist));
        tasks.add(new AzureTask<Void>(app::reset));
        return tasks;
    }

    @Override
    public SpringCloudDeployment execute() {
        this.subTasks.forEach(t -> t.getSupplier().get());
        return this.deployment;
    }
}
