/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.model.AzureRemotableArtifact;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.service.SpringCloudDeploymentManager;
import com.microsoft.azure.tools.utils.RxUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class SpringCloudDeployment implements IAzureEntityManager<SpringCloudDeploymentEntity> {
    @Getter
    private final SpringCloudApp app;
    private final SpringCloudDeploymentEntity local;
    private SpringCloudDeploymentEntity remote;
    private final SpringCloudDeploymentManager deploymentManager;
    private boolean refreshed;

    public SpringCloudDeployment(SpringCloudDeploymentEntity deployment, SpringCloudApp app) {
        this.app = app;
        this.local = deployment;
        this.deploymentManager = new SpringCloudDeploymentManager(app.getCluster().getClient());
    }

    public boolean exists() {
        if (!this.refreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    public SpringCloudDeploymentEntity entity() {
        return Objects.nonNull(this.remote) ? this.remote : this.local;
    }

    public SpringCloudApp app() {
        return this.app;
    }

    public SpringCloudDeployment start() {
        final SpringCloudDeploymentEntity deployment = this.entity();
        this.deploymentManager.start(deployment.getName(), deployment.getApp());
        return this;
    }

    @Nonnull
    public SpringCloudDeployment refresh() {
        final String deploymentName = this.name();
        final SpringCloudAppEntity app = this.app.entity();
        this.remote = this.deploymentManager.get(deploymentName, app);
        this.refreshed = true;
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

    public SpringCloudDeployment scale(final ScaleSettings scaleSettings) {
        return this.update().configScaleSettings(scaleSettings).commit();
    }

    public static class Updater implements ICommittable<SpringCloudDeployment> {
        protected final DeploymentResourceInner resource;
        protected final SpringCloudDeployment deployment;
        protected AzureRemotableArtifact delayableArtifact;

        public Updater(SpringCloudDeployment deployment) {
            this.deployment = deployment;
            this.resource = new DeploymentResourceInner();
        }

        public Updater configEnvironmentVariables(Map<String, String> env) {
            final Map<String, String> oldEnv = Optional.ofNullable(this.deployment.remote)
                .map(e -> e.getInner().properties().deploymentSettings().environmentVariables()).orElse(null);
            if (MapUtils.isNotEmpty(env) && !Objects.equals(env, oldEnv)) {
                AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment)
                    .withEnvironmentVariables(env);
            }
            return this;
        }

        public Updater configJvmOptions(String jvmOptions) {
            final String oldJvmOptions = Optional.ofNullable(this.deployment.remote)
                .map(e -> e.getInner().properties().deploymentSettings().jvmOptions()).orElse(null);
            if (StringUtils.isNotBlank(jvmOptions) && !Objects.equals(jvmOptions, oldJvmOptions)) {
                AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment)
                    .withJvmOptions(jvmOptions.trim());
            }
            return this;
        }

        public Updater configRuntimeVersion(String version) {
            final RuntimeVersion oldRuntimeVersion = Optional.ofNullable(this.deployment.remote)
                .map(e -> e.getInner().properties().deploymentSettings().runtimeVersion()).orElse(null);
            if (Objects.nonNull(version) && !Objects.equals(oldRuntimeVersion, RuntimeVersion.fromString(version))) {
                AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment)
                    .withRuntimeVersion(RuntimeVersion.fromString(version));
            }
            return this;
        }

        public Updater configArtifact(AzureRemotableArtifact artifact) {
            this.delayableArtifact = artifact;
            final String oldPath = Optional.ofNullable(this.deployment.remote)
                .map(e -> e.getInner().properties().source().relativePath()).orElse(null);
            if (Objects.nonNull(artifact) && Objects.nonNull(artifact.getRemotePath()) && !Objects.equals(artifact.getRemotePath(), oldPath)) {
                AzureSpringCloudConfigUtils.getOrCreateSource(this.resource, this.deployment)
                    .withRelativePath(artifact.getRemotePath());
            }
            return this;
        }

        public Updater configScaleSettings(ScaleSettings newSettings) {
            final ScaleSettings oldSettings = Optional.ofNullable(this.deployment.remote)
                .map(SpringCloudDeploymentEntity::getInner).map(AzureSpringCloudConfigUtils::getScaleSettings).orElse(null);
            if (!AzureSpringCloudConfigUtils.isEmpty(newSettings) && !Objects.equals(newSettings, oldSettings)) {
                AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(this.resource, this.deployment)
                    .withCpu(newSettings.getCpu()).withMemoryInGB(newSettings.getMemoryInGB());
                AzureSpringCloudConfigUtils.getOrCreateSku(this.resource, this.deployment)
                    .withCapacity(newSettings.getCapacity());
            }
            return this;
        }

        public SpringCloudDeployment commit() {
            // FIXME: start workaround for bug: can not scale when updating other properties
            final ScaleSettings scaleSettings = AzureSpringCloudConfigUtils.getScaleSettings(this.resource);
            if (Objects.nonNull(scaleSettings) && !AzureSpringCloudConfigUtils.isEmpty(scaleSettings)) {
                this.scale(scaleSettings);
            }
            // end workaround;
            this.configArtifact(this.delayableArtifact);
            if (Objects.isNull(this.resource.properties()) && Objects.isNull(this.resource.sku())) {
                log.info("Skip updating deployment({}) since its properties is not changed.", this.deployment.name());
                return this.deployment;
            }
            this.deployment.remote = this.deployment.deploymentManager.update(this.resource, this.deployment.entity());
            return this.deployment.start();
        }

        private void scale(@Nonnull ScaleSettings scaleSettings) {
            log.info("Scaling deployment({})...", TextUtils.cyan(this.deployment.name()));
            final DeploymentResourceInner tempResource = new DeploymentResourceInner();
            AzureSpringCloudConfigUtils.getOrCreateDeploymentSettings(tempResource, this.deployment)
                .withCpu(scaleSettings.getCpu()).withMemoryInGB(scaleSettings.getMemoryInGB());
            AzureSpringCloudConfigUtils.getOrCreateSku(tempResource, this.deployment)
                .withCapacity(scaleSettings.getCapacity());
            this.deployment.remote = this.deployment.deploymentManager.update(tempResource, this.deployment.entity());
            log.info("Successfully scaled the deployment.");
        }
    }

    public static class Creator extends Updater {
        public Creator(SpringCloudDeployment manager) {
            super(manager);
        }

        public SpringCloudDeployment commit() {
            this.configArtifact(this.delayableArtifact);
            final String deploymentName = this.deployment.name();
            final SpringCloudAppEntity app = this.deployment.app.entity();
            this.deployment.remote = this.deployment.deploymentManager.create(this.resource, deploymentName, app);
            return this.deployment.start();
        }
    }
}
