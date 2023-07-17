/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterDraft;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudClusterConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private static final String START_APP = "Starting Spring App after deploying artifacts...";
    private Disposable disposable;
    public DeploySpringCloudAppTask(SpringCloudAppConfig appConfig) {
        this(appConfig, false, false);
    }

    public DeploySpringCloudAppTask(SpringCloudAppConfig appConfig, boolean openStreamingLogOnFailure, boolean waitDeploymentComplete) {
        this.config = appConfig;
        this.subTasks = new ArrayList<>();
        this.openStreamingLogOnFailure = openStreamingLogOnFailure;
        this.waitDeploymentComplete = waitDeploymentComplete;
        this.initTasks();
    }

    private void initTasks() {
        // Init spring clients, and prompt users to confirm
        final SpringCloudDeploymentConfig deploymentConfig = config.getDeployment();
        final SpringCloudClusterConfig clusterConfig = config.getCluster();
        final String subscriptionId = Optional.ofNullable(clusterConfig).map(SpringCloudClusterConfig::getSubscriptionId).filter(StringUtils::isNotBlank).orElseThrow(() -> new AzureToolkitRuntimeException("'subscriptionId' is required"));
        final String clusterName = Optional.ofNullable(config.getCluster()).map(SpringCloudClusterConfig::getClusterName).filter(StringUtils::isNotBlank).orElseThrow(() -> new AzureToolkitRuntimeException("'clusterName' is required"));
        final String appName = Optional.ofNullable(config.getAppName()).filter(StringUtils::isNotBlank).orElseThrow(() -> new AzureToolkitRuntimeException("'appName' is required"));
        final String resourceGroup = Optional.ofNullable(clusterConfig).map(SpringCloudClusterConfig::getResourceGroup).orElse(null);
        final SpringCloudCluster cluster = Azure.az(AzureSpringCloud.class).clusters(subscriptionId).getOrDraft(clusterName, resourceGroup);
        final SpringCloudAppDraft app = cluster.apps().updateOrCreate(appName, resourceGroup);
        final String deploymentName = StringUtils.firstNonBlank(
            deploymentConfig.getDeploymentName(),
            config.getActiveDeploymentName(),
            app.getActiveDeploymentName(),
            DEFAULT_DEPLOYMENT_NAME
        );
        final boolean toCreateCluster = cluster.isDraftForCreating() && !cluster.exists();

        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !toCreateApp && !app.deployments().exists(deploymentName, resourceGroup);
        config.setActiveDeploymentName(StringUtils.firstNonBlank(app.getActiveDeploymentName(), toCreateApp || toCreateDeployment ? deploymentName : null));

        OperationContext.action().setTelemetryProperty("subscriptionId", subscriptionId);
        OperationContext.current().setTelemetryProperty("isCreateNewApp", String.valueOf(toCreateApp));
        OperationContext.current().setTelemetryProperty("isCreateDeployment", String.valueOf(toCreateApp || toCreateDeployment));
        OperationContext.current().setTelemetryProperty("isDeploymentNameGiven", String.valueOf(StringUtils.isNotEmpty(deploymentConfig.getDeploymentName())));

        final AzureString CREATE_APP_TITLE = AzureString.format("Create new app({0}) and deployment({1}) in Azure Spring Apps({2})", appName, deploymentName, clusterName);
        final AzureString UPDATE_APP_TITLE = AzureString.format("Update app({0}) of Azure Spring Apps({1})", appName, clusterName);
        final AzureString CREATE_DEPLOYMENT_TITLE = AzureString.format("Create new deployment({0}) in app({1})", deploymentName, appName);
        final AzureString UPDATE_DEPLOYMENT_TITLE = AzureString.format("Update deployment({0}) of app({1})", deploymentName, appName);
        final AzureString MODIFY_DEPLOYMENT_TITLE = toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE;

        if (toCreateCluster) {
            addCreateClusterTask(cluster);
        }
        app.setConfig(config);
        if (toCreateApp) {
            this.subTasks.add(new AzureTask<Void>(CREATE_APP_TITLE, app::createIfNotExist));
        }
        this.subTasks.add(new AzureTask<Void>(MODIFY_DEPLOYMENT_TITLE, () -> {
            final SpringCloudDeploymentDraft draft = app.deployments().updateOrCreate(deploymentName, resourceGroup);
            draft.setConfig(config.getDeployment());
            try {
                this.deployment = draft.commit();
            } catch (final Exception e) {
                app.refresh();
                this.deployment = app.getActiveDeployment();
                throw new AzureToolkitRuntimeException(e);
            }
        }));
        this.subTasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, () -> {
            final SpringCloudAppDraft draft = (SpringCloudAppDraft) app.update();
            draft.setConfig(config);
            draft.updateIfExist();
            app.refresh();
        }));
        this.subTasks.add(new AzureTask<Void>(app::reset));
        if (this.waitDeploymentComplete) {
            this.subTasks.add(new AzureTask<Void>(this::startApp));
        }
    }

    private void addCreateClusterTask(SpringCloudCluster cluster) {
        Optional.ofNullable(config.getResourceGroup()).filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new AzureToolkitRuntimeException("'resourceGroup' is required to create Azure Spring Apps"));
        Optional.ofNullable(config.getCluster()).map(SpringCloudClusterConfig::getRegion).filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new AzureToolkitRuntimeException("'region' is required to create Azure Spring Apps"));
        Optional.ofNullable(config.getCluster()).map(SpringCloudClusterConfig::getSku).filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new AzureToolkitRuntimeException("'sku' is required to create Azure Spring Apps"));
        final SpringCloudClusterConfig clusterConfig = config.getCluster();
        addCreateResourceGroupTaskIfNecessary(clusterConfig);
        addCreateEnvironmentTaskIfNecessary(clusterConfig);
        final AzureString CREATE_CLUSTER_TITLE = AzureString.format("Create new Azure Spring Apps({0})", clusterConfig.getClusterName());
        this.subTasks.add(new AzureTask<Void>(CREATE_CLUSTER_TITLE, () -> {
            final SpringCloudClusterDraft draft = (SpringCloudClusterDraft) cluster;
            final SpringCloudClusterDraft.Config config = getDraftConfig(DeploySpringCloudAppTask.this.config.getCluster());
            draft.setConfig(config);
            draft.createIfNotExist();
        }));
    }

    private void addCreateResourceGroupTaskIfNecessary(@Nonnull final SpringCloudClusterConfig config) {
        final ResourceGroup resourceGroup = Azure.az(AzureResources.class).groups(config.getSubscriptionId())
            .getOrDraft(config.getResourceGroup(), config.getResourceGroup());
        if (resourceGroup.isDraftForCreating() && !resourceGroup.exists()) {
            final AzureString title = AzureString.format("Create new resource group ({0})", config.getResourceGroup());
            final ResourceGroupDraft draft = (ResourceGroupDraft) resourceGroup;
            draft.setRegion(Region.fromName(config.getRegion()));
            this.subTasks.add(new AzureTask<Void>(title, draft::commit));
        }
    }

    private void addCreateEnvironmentTaskIfNecessary(@Nonnull final SpringCloudClusterConfig clusterConfig) {
        final Sku sku = Sku.fromString(config.getCluster().getSku());
        if (!sku.isConsumptionTier()) {
            return;
        }
        final String env = Optional.ofNullable(config.getCluster()).map(SpringCloudClusterConfig::getEnvironment).filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new AzureToolkitRuntimeException("'environment' is required to create Azure Spring Apps"));
        final ContainerAppsEnvironment environment = Azure.az(AzureContainerApps.class).environments(clusterConfig.getSubscriptionId())
            .getOrDraft(env, StringUtils.firstNonBlank(clusterConfig.getEnvironmentResourceGroup(), clusterConfig.getResourceGroup()));
        final AzureString title = AzureString.format("Create new Container Apps Environment({0})", environment.getName());
        if (environment.isDraftForCreating() && !environment.exists()) {
            this.subTasks.add(new AzureTask<Void>(title, () -> {
                final ResourceGroup resourceGroup = Azure.az(AzureResources.class).groups(config.getSubscriptionId())
                    .get(config.getResourceGroup(), config.getResourceGroup());
                final ContainerAppsEnvironmentDraft draft = (ContainerAppsEnvironmentDraft) environment;
                final ContainerAppsEnvironmentDraft.Config config = new ContainerAppsEnvironmentDraft.Config();
                config.setName(draft.getName());
                config.setResourceGroup(resourceGroup);
                config.setRegion(Region.fromName(clusterConfig.getRegion()));
                draft.setConfig(config);
                draft.commit();
            }));
        }
    }

    private static SpringCloudClusterDraft.Config getDraftConfig(@Nonnull final SpringCloudClusterConfig cluster) {
        final SpringCloudClusterDraft.Config result = new SpringCloudClusterDraft.Config();
        final ResourceGroup resourceGroup = Azure.az(AzureResources.class).groups(cluster.getSubscriptionId())
            .get(cluster.getResourceGroup(), cluster.getResourceGroup());
        final Sku sku = Sku.fromString(cluster.getSku());
        result.setName(cluster.getClusterName());
        result.setResourceGroup(resourceGroup);
        result.setRegion(com.azure.core.management.Region.fromName(cluster.getRegion()));
        result.setSku(sku);
        if (sku.isConsumptionTier()) {
            final ContainerAppsEnvironment environment = Azure.az(AzureContainerApps.class).environments(cluster.getSubscriptionId())
                .get(cluster.getEnvironment(), StringUtils.firstNonBlank(cluster.getEnvironmentResourceGroup(), cluster.getResourceGroup()));
            result.setManagedEnvironmentId(Objects.requireNonNull(environment).getId());
        }
        return result;
    }

    @Override
    @AzureOperation(name = "internal/springcloud.create_update_app.app", params = {"this.config.getAppName()"})
    public SpringCloudDeployment doExecute() throws Exception {
        for (final AzureTask<?> t : this.subTasks) {
            t.getBody().call();
        }
        return this.deployment;
    }

    private void startApp() {
        AzureMessager.getMessager().info(START_APP);
        if (!this.deployment.waitUntilReady(TIMEOUT_IN_SECONDS)) {
            AzureMessager.getMessager().warning(GET_APP_STATUS_TIMEOUT);
            if (Objects.nonNull(this.deployment) && openStreamingLogOnFailure) {
                this.deployment.startStreamingLog(false);
            }
        }
    }
}
