/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeployment implements IAzureEntityManager<SpringCloudDeploymentEntity> {
    @Getter
    @Nonnull
    final SpringCloudApp app;
    @Nonnull
    private final SpringCloudDeploymentEntity local;
    @Nullable
    private SpringAppDeployment remote;
    private boolean refreshed;

    public SpringCloudDeployment(@Nonnull SpringCloudDeploymentEntity deployment, @Nonnull SpringCloudApp app) {
        this.local = deployment;
        this.remote = this.local.getRemote();
        this.app = app;
    }

    public boolean exists() {
        if (Objects.isNull(this.remote) && !this.refreshed) {
            this.refresh();
        }
        return Objects.nonNull(this.remote);
    }

    @Nonnull
    public SpringCloudDeploymentEntity entity() {
        final SpringAppDeployment remote = Objects.nonNull(this.remote) ? this.remote : this.local.getRemote();
        if (Objects.isNull(remote)) {
            return this.local;
        }
        // prevent inconsistent properties between local and remote when local's properties is modified.
        return new SpringCloudDeploymentEntity(remote, this.app.entity());
    }

    @Nonnull
    public SpringCloudApp app() {
        return this.app;
    }

    @AzureOperation(name = "springcloud|deployment.start", params = {"this.entity().getName()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment start() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote).start();
        }
        return this;
    }

    @AzureOperation(name = "springcloud|deployment.stop", params = {"this.entity().getName()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment stop() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote).stop();
        }
        return this;
    }

    @AzureOperation(name = "springcloud|deployment.restart", params = {"this.entity().getName()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment restart() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote).restart();
        }
        return this;
    }

    @Nonnull
    public SpringCloudDeployment refresh() {
        final SpringCloudDeployment _this = this.app.deployment(this.entity().getName());
        this.updateRemote(_this.entity().getRemote());
        this.refreshed = true;
        return this;
    }

    private void updateRemote(SpringAppDeployment remote) {
        this.remote = remote;
        this.local.setRemote(this.remote);
    }

    public boolean waitUntilReady(int timeoutInSeconds) {
        AzureMessager.getMessager().info("Getting deployment status...");
        final SpringCloudDeployment deployment = Utils.pollUntil(this::refresh, Utils::isDeploymentDone, timeoutInSeconds);
        return Utils.isDeploymentDone(deployment);
    }

    public Creator create() {
        assert this.app.exists() : String.format("app(%s) not exist", this.app.name());
        return new Creator(this);
    }

    public Updater update() {
        assert this.exists() : String.format("deployment(%s) not exist", this.name());
        return new Updater(this);
    }

    public SpringCloudDeployment scale(final ScaleSettings scaleSettings) {
        return this.update().configScaleSettings(scaleSettings).commit();
    }

    public abstract static class Modifier implements ICommittable<SpringCloudDeployment> {
        protected final SpringCloudDeployment deployment;
        protected SpringAppDeploymentImpl modifier;
        protected ScaleSettings newScaleSettings;
        protected boolean skippable = true;

        public Modifier(SpringCloudDeployment deployment) {
            this.deployment = deployment;
        }

        public Modifier configEnvironmentVariables(@Nullable Map<String, String> env) {
            final Map<String, String> oldEnv = Optional.ofNullable(this.modifier.settings()).map(DeploymentSettings::environmentVariables).orElse(null);
            if (!Objects.equals(env, oldEnv) && Objects.nonNull(env)) {
                this.skippable = false;
                env.forEach((key, value) -> {
                    if (StringUtils.isBlank(value)) {
                        this.modifier.withoutEnvironment(key);
                    } else {
                        this.modifier.withEnvironment(key, value);
                    }
                });
            }
            return this;
        }

        public Modifier configJvmOptions(@Nullable String jvmOptions) {
            final String oldJvmOptions = Optional.ofNullable(this.modifier.settings()).map(DeploymentSettings::jvmOptions).orElse(null);
            if (!Objects.equals(jvmOptions, oldJvmOptions)) {
                this.skippable = false;
                this.modifier.withJvmOptions(jvmOptions);
            }
            return this;
        }

        public Modifier configRuntimeVersion(String version) {
            final RuntimeVersion oldRuntimeVersion = Optional.ofNullable(this.modifier.settings()).map(DeploymentSettings::runtimeVersion).orElse(null);
            if (StringUtils.isBlank(version)) {
                this.skippable = false;
                this.modifier.withRuntime(RuntimeVersion.JAVA_8);
            } else if (!Objects.equals(oldRuntimeVersion, RuntimeVersion.fromString(version))) {
                this.skippable = false;
                this.modifier.withRuntime(RuntimeVersion.fromString(version));
            }
            return this;
        }

        public Modifier configArtifact(File artifact) {
            if (Objects.nonNull(artifact)) {
                this.skippable = false;
                this.modifier.withJarFile(artifact);
            }
            return this;
        }

        public Modifier configScaleSettings(ScaleSettings newSettings) {
            final Optional<DeploymentSettings> settings = Optional.ofNullable(this.modifier.settings());
            final ScaleSettings oldSettings = ScaleSettings.builder()
                .cpu(settings.map(DeploymentSettings::cpu).orElse(null))
                .memoryInGB(settings.map(DeploymentSettings::memoryInGB).orElse(null))
                .capacity(Optional.ofNullable(this.modifier.innerModel().sku()).map(Sku::capacity).orElse(null))
                .build();
            if (!newSettings.isEmpty() && !oldSettings.equals(newSettings)) {
                // Deployment cannot be scaled and updated at the same time. so should not set properties directly on modifier.
                this.newScaleSettings = newSettings;
            }
            return this;
        }
    }

    public static class Updater extends Modifier {
        public Updater(SpringCloudDeployment deployment) {
            super(deployment);
            this.modifier = ((SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.remote).update());
        }

        @AzureOperation(name = "springcloud|deployment.update", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        public SpringCloudDeployment commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            if (this.skippable) {
                messager.info(String.format("Skip updating deployment(%s) since its properties is not changed.", this.deployment.name()));
            } else {
                messager.info(String.format("Start updating deployment(%s)...", messager.value(this.deployment.name())));
                this.deployment.updateRemote(this.modifier.apply());
                messager.success(String.format("Deployment(%s) is successfully updated", messager.value(this.deployment.name())));
            }
            return this.scale(this.newScaleSettings);
        }

        @AzureOperation(name = "springcloud|deployment.scale", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        public SpringCloudDeployment scale(ScaleSettings settings) {
            if (Objects.isNull(settings) || settings.isEmpty()) {
                return this.deployment;
            }
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start scaling deployment(%s)...", messager.value(this.deployment.name())));
            final SpringAppDeploymentImpl modifier = ((SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.remote).update());
            modifier.withCpu(settings.getCpu()).withMemory(settings.getMemoryInGB()).withInstance(settings.getCapacity());
            this.deployment.updateRemote(modifier.apply());
            messager.success(String.format("Deployment(%s) is successfully scaled.", messager.value(this.deployment.name())));
            return this.deployment;
        }
    }

    public static class Creator extends Modifier {
        public Creator(SpringCloudDeployment deployment) {
            super(deployment);
            this.modifier = (SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.app.getRemote()).deployments().define(deployment.name());
        }

        @AzureOperation(name = "springcloud|deployment.create", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        public SpringCloudDeployment commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start creating deployment(%s)...", messager.value(this.deployment.name())));
            final ScaleSettings settings = this.newScaleSettings;
            if (Objects.nonNull(settings) && !settings.isEmpty()) {
                modifier.withCpu(settings.getCpu()).withMemory(settings.getMemoryInGB()).withInstance(settings.getCapacity());
            }
            this.deployment.updateRemote(this.modifier.create());
            messager.success(String.format("Deployment(%s) is successfully created", messager.value(this.deployment.name())));
            return this.deployment.start();
        }
    }
}
