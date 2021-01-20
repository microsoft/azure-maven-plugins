/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentSettings;
import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2020_07_01.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.SkuInner;
import com.microsoft.azure.toolkit.lib.common.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudRuntimeVersion;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudDeploymentManager;
import com.microsoft.azure.tools.utils.RxUtils;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class SpringCloudDeployment implements IAzureEntityManager<SpringCloudDeploymentEntity> {
    @Getter
    private final SpringCloudApp app;
    private final SpringCloudDeploymentEntity local;
    private SpringCloudDeploymentEntity remote;
    private final SpringCloudDeploymentManager deploymentManager;

    public SpringCloudDeployment(SpringCloudDeploymentEntity deployment, SpringCloudApp app) {
        this.app = app;
        this.local = deployment;
        this.deploymentManager = new SpringCloudDeploymentManager(app.getCluster().getClient());
    }

    public boolean exists() {
        this.refresh();
        return Objects.nonNull(this.remote);
    }

    public SpringCloudDeploymentEntity entity() {
        return Objects.nonNull(this.remote) ? this.remote : this.local;
    }

    public SpringCloudApp app() {
        return this.app;
    }

    public SpringCloudDeployment start() {
        this.deploymentManager.start(this.entity());
        return this;
    }

    @Nonnull
    public SpringCloudDeployment refresh() {
        final String deploymentName = this.entity().getName();
        final SpringCloudAppEntity app = this.app.entity();
        this.remote = this.deploymentManager.get(deploymentName, app);
        return this;
    }

    public boolean waitUntilReady(int timeoutInSeconds) {
        final SpringCloudDeployment deployment = RxUtils.pollUntil(this::refresh, AzureSpringCloudConfigUtils::isDeploymentDone, timeoutInSeconds);
        return AzureSpringCloudConfigUtils.isDeploymentDone(deployment);
    }

    public Creator create() {
        return new Creator(this);
    }

    public Updater update() {
        return new Updater(this);
    }

    public static class Updater {
        protected final DeploymentResourceInner resource;
        protected final SpringCloudDeployment deployment;

        public Updater(SpringCloudDeployment deployment) {
            this.deployment = deployment;
            this.resource = new DeploymentResourceInner();
        }

        public Updater configEnvironmentVariables(Map<String, String> env) {
            final DeploymentSettings settings = AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment);
            settings.withEnvironmentVariables(env);
            return this;
        }

        public Updater configJvmOptions(String jvmOptions) {
            final DeploymentSettings settings = AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment);
            settings.withJvmOptions(jvmOptions);
            return this;
        }

        public Updater configScaleSettings(ScaleSettings scale) {
            final DeploymentSettings settings = AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment);
            final SkuInner sku = AzureSpringCloudConfigUtils.getOrCreateSku(this.resource, this.deployment);
            settings.withCpu(scale.getCpu()).withMemoryInGB(scale.getMemoryInGB());
            sku.withCapacity(scale.getCapacity());
            return this;
        }

        public Updater configRuntimeVersion(SpringCloudRuntimeVersion version) {
            final DeploymentSettings settings = AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment);
            settings.withRuntimeVersion(RuntimeVersion.fromString(version.toString()));
            return this;
        }

        public Updater configAppArtifactPath(String path) {
            final UserSourceInfo source = AzureSpringCloudConfigUtils.getOrCreateSource(this.resource, this.deployment);
            source.withRelativePath(path);
            return this;
        }

        public SpringCloudDeployment commit() {
            this.deployment.remote = this.deployment.deploymentManager.update(this.resource, this.deployment.entity());
            return this.deployment.start();
        }
    }

    public static class Creator extends Updater {
        public Creator(SpringCloudDeployment manager) {
            super(manager);
        }

        public SpringCloudDeployment commit() {
            final String deploymentName = this.deployment.entity().getName();
            final SpringCloudAppEntity app = this.deployment.app.entity();
            this.deployment.remote = this.deployment.deploymentManager.create(this.resource, deploymentName, app);
            return this.deployment.start();
        }
    }
}
