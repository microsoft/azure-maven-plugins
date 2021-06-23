/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeployment extends AbstractAzureEntityManager<SpringCloudDeployment, SpringCloudDeploymentEntity, SpringAppDeployment>
        implements AzureOperationEvent.Source<SpringCloudDeployment> {
    @Getter
    @Nonnull
    final SpringCloudApp app;

    public SpringCloudDeployment(@Nonnull SpringCloudDeploymentEntity deployment, @Nonnull SpringCloudApp app) {
        super(deployment);
        this.app = app;
    }

    @Override
    @AzureOperation(name = "springcloud|deployment.load", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    SpringAppDeployment loadRemote() {
        try {
            return Objects.requireNonNull(this.app.remote()).deployments().getByName(this.name());
        } catch (ManagementException e) { // if cluster with specified resourceGroup/name removed.
            if (HttpStatus.SC_NOT_FOUND == e.getResponse().getStatusCode()) {
                return null;
            }
            throw e;
        }
    }

    @Nonnull
    public SpringCloudApp app() {
        return this.app;
    }

    @AzureOperation(name = "springcloud|deployment.start", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment start() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote()).start();
        }
        return this;
    }

    @AzureOperation(name = "springcloud|deployment.stop", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment stop() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote()).stop();
        }
        return this;
    }

    @AzureOperation(name = "springcloud|deployment.restart", params = {"this.entity().getName()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public SpringCloudDeployment restart() {
        if (this.exists()) {
            Objects.requireNonNull(this.remote()).restart();
        }
        return this;
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

    public abstract static class Modifier implements ICommittable<SpringCloudDeployment>, AzureOperationEvent.Source<SpringCloudDeployment> {
        protected final SpringCloudDeployment deployment;
        protected SpringAppDeploymentImpl modifier;
        protected ScaleSettings newScaleSettings;
        protected boolean skippable = true;

        public Modifier(SpringCloudDeployment deployment) {
            this.deployment = deployment;
        }

        public Modifier configEnvironmentVariables(@Nullable Map<String, String> env) {
            final Map<String, String> oldEnv = Optional.ofNullable(this.modifier.settings()).map(DeploymentSettings::environmentVariables).orElse(null);
            final boolean allEmpty = MapUtils.isEmpty(env) && MapUtils.isEmpty(oldEnv);
            if (!allEmpty && !Objects.equals(env, oldEnv) && Objects.nonNull(env)) {
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
            final String newJvmOptions = Optional.ofNullable(jvmOptions).map(String::trim).orElse("");
            final String oldJvmOptions = Optional.ofNullable(this.modifier.settings()).map(DeploymentSettings::jvmOptions).orElse(null);
            if (!StringUtils.isAllBlank(newJvmOptions, oldJvmOptions) && !Objects.equals(newJvmOptions, oldJvmOptions)) {
                this.skippable = false;
                this.modifier.withJvmOptions(newJvmOptions);
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

        @AzureOperation(name = "springcloud|deployment.scale", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        protected SpringCloudDeployment scale(ScaleSettings settings) {
            if (Objects.isNull(settings) || settings.isEmpty()) {
                return this.deployment;
            }
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start scaling deployment(%s)...", messager.value(this.deployment.name())));
            final SpringAppDeploymentImpl modifier = ((SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.remote()).update());
            modifier.withCpu(settings.getCpu()).withMemory(settings.getMemoryInGB()).withInstance(settings.getCapacity());
            this.deployment.entity.setRemote(modifier.apply());
            messager.success(String.format("Deployment(%s) is successfully scaled.", messager.value(this.deployment.name())));
            return this.deployment;
        }

        @NotNull
        @Override
        public AzureOperationEvent.Source<SpringCloudDeployment> getEventSource() {
            return this.deployment;
        }
    }

    public static class Updater extends Modifier {
        public Updater(SpringCloudDeployment deployment) {
            super(deployment);
            this.modifier = ((SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.remote()).update());
        }

        @AzureOperation(name = "springcloud|deployment.update", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        public SpringCloudDeployment commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            if (this.skippable) {
                messager.info(String.format("Skip updating deployment(%s) since its properties is not changed.", this.deployment.name()));
            } else {
                messager.info(String.format("Start updating deployment(%s)...", messager.value(this.deployment.name())));
                this.deployment.refresh(this.modifier.apply());
                messager.success(String.format("Deployment(%s) is successfully updated", messager.value(this.deployment.name())));
            }
            return this.scale(this.newScaleSettings);
        }
    }

    public static class Creator extends Modifier {
        public Creator(SpringCloudDeployment deployment) {
            super(deployment);
            this.modifier = (SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.app.remote()).deployments().define(deployment.name());
        }

        @AzureOperation(name = "springcloud|deployment.create", params = {"this.deployment.name()", "this.deployment.app.name()"}, type = AzureOperation.Type.SERVICE)
        public SpringCloudDeployment commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(String.format("Start creating deployment(%s)...", messager.value(this.deployment.name())));
            this.deployment.refresh(this.modifier.create());
            messager.success(String.format("Deployment(%s) is successfully created", messager.value(this.deployment.name())));
            return this.scale(this.newScaleSettings);
        }
    }
}
