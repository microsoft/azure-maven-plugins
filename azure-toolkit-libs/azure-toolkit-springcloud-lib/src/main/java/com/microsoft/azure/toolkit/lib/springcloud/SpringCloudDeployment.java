/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.resources.fluentcore.model.Refreshable;
import com.google.common.base.Charsets;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class SpringCloudDeployment extends AbstractAzureResource<SpringCloudDeployment, SpringCloudDeploymentEntity, SpringAppDeployment>
        implements AzureOperationEvent.Source<SpringCloudDeployment> {
    @Getter
    @Nonnull
    final SpringCloudApp app;

    public SpringCloudDeployment(@Nonnull SpringCloudDeploymentEntity deployment, @Nonnull SpringCloudApp app) {
        super(deployment);
        this.app = app;
    }

    @Override
    @AzureOperation(name = "springcloud.load_deployment.deployment&app", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    protected SpringAppDeployment loadRemote() {
        try {
            return Optional.ofNullable(this.app.remote()).map(r -> r.deployments().getByName(this.name())).orElse(null);
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

    @AzureOperation(name = "springcloud.start_deployment.deployment&app", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        if (this.exists()) {
            this.status(Status.PENDING);
            Objects.requireNonNull(this.remote()).start();
            this.refreshStatus();
        }
    }

    @AzureOperation(name = "springcloud.stop_deployment.deployment&app", params = {"this.name()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        if (this.exists()) {
            this.status(Status.PENDING);
            Objects.requireNonNull(this.remote()).stop();
            this.refreshStatus();
        }
    }

    @AzureOperation(name = "springcloud.restart_deployment.deployment&app", params = {"this.entity().getName()", "this.app.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        if (this.exists()) {
            this.status(Status.PENDING);
            Objects.requireNonNull(this.remote()).restart();
            this.refreshStatus();
        }
    }

    @AzureOperation(
            name = "springcloud.wait_until_deployment_ready.deployment&app",
            params = {"this.entity().getName()", "this.app.name()"},
            type = AzureOperation.Type.SERVICE
    )
    public boolean waitUntilReady(int timeoutInSeconds) {
        AzureMessager.getMessager().info("Getting deployment status...");
        final SpringCloudDeployment deployment = Utils.pollUntil(this::refresh, Utils::isDeploymentDone, timeoutInSeconds);
        return Utils.isDeploymentDone(deployment);
    }

    @Override
    protected String loadStatus() {
        Optional.ofNullable(this.entity().getRemote()).ifPresent(Refreshable::refresh);
        switch (this.entity().getStatus()) {
            case RUNNING:
                return Status.RUNNING;
            case STOPPED:
                return Status.STOPPED;
            case FAILED:
                return Status.ERROR;
            case ALLOCATING:
            case UPGRADING:
            case COMPILING:
                return Status.PENDING;
            default:
                return Status.UNKNOWN;
        }
    }

    public Creator create() {
        assert this.app.exists() : String.format("app(%s) not exist", this.app.name());
        return new Creator(this);
    }

    public Updater update() {
        assert this.exists() : String.format("deployment(%s) not exist", this.name());
        return new Updater(this);
    }

    @SneakyThrows
    public Flux<String> streamLogs(final String instance) {
        return streamLogs(instance, 0, 10, 0, true);
    }

    @SneakyThrows
    public Flux<String> streamLogs(final String instance, int sinceSeconds, int tailLines, int limitBytes, boolean follow) {
        final HttpClient client = HttpClient.create().keepAlive(true);
        final URIBuilder endpoint = new URIBuilder(app.entity().getLogStreamingEndpoint(instance));
        endpoint.addParameter("follow", String.valueOf(follow));
        if (sinceSeconds > 0) {
            endpoint.addParameter("sinceSeconds", String.valueOf(sinceSeconds));
        }
        if (tailLines > 0) {
            endpoint.addParameter("tailLines", String.valueOf(tailLines));
        }
        if (limitBytes > 0) {
            endpoint.addParameter("limitBytes", String.valueOf(limitBytes));
        }
        final String password = app.getCluster().entity().getTestKey();
        final String userPass = "primary:" + password;
        final String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
        final Consumer<? super HttpHeaders> headerBuilder = header -> header.set("Authorization", basicAuth);
        return client.headers(headerBuilder)
                .responseTimeout(Duration.of(10, ChronoUnit.MINUTES))
                .get()
                .uri(endpoint.build())
                .response((resp, cont) -> resp.status().code() == 200 ? cont.asString(Charsets.UTF_8) : Mono.empty());
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

        @AzureOperation(
                name = "springcloud.scale_deployment.deployment&app",
                params = {"this.deployment.name()", "this.deployment.app.name()"},
                type = AzureOperation.Type.SERVICE
        )
        protected SpringCloudDeployment scale(ScaleSettings settings) {
            if (Objects.isNull(settings) || settings.isEmpty()) {
                return this.deployment;
            }
            this.deployment.status(Status.PENDING);
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start scaling deployment({0})...", this.deployment.name()));
            final SpringAppDeploymentImpl m = ((SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.remote()).update());
            m.withCpu(settings.getCpu()).withMemory(settings.getMemoryInGB()).withInstance(settings.getCapacity());
            this.deployment.entity.setRemote(m.apply());
            messager.success(AzureString.format("Deployment({0}) is successfully scaled.", this.deployment.name()));
            this.deployment.app.refresh();
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

        @AzureOperation(
                name = "springcloud.update_deployment.deployment&app",
                params = {"this.deployment.name()", "this.deployment.app.name()"},
                type = AzureOperation.Type.SERVICE
        )
        public SpringCloudDeployment commit() {
            final IAzureMessager messager = AzureMessager.getMessager();
            if (this.skippable) {
                messager.info(AzureString.format("Skip updating deployment({0}) since its properties is not changed.", this.deployment.name()));
            } else {
                this.deployment.status(Status.PENDING);
                messager.info(AzureString.format("Start updating deployment({0})...", this.deployment.name()));
                this.deployment.refresh(this.modifier.apply());
                messager.success(AzureString.format("Deployment({0}) is successfully updated", this.deployment.name()));
                this.deployment.refreshStatus();
            }
            return this.scale(this.newScaleSettings);
        }
    }

    public static class Creator extends Modifier {
        public Creator(SpringCloudDeployment deployment) {
            super(deployment);
            this.modifier = (SpringAppDeploymentImpl) Objects.requireNonNull(this.deployment.app.remote()).deployments().define(deployment.name());
        }

        @AzureOperation(
                name = "springcloud.create_deployment.deployment&app",
                params = {"this.deployment.name()", "this.deployment.app.name()"},
                type = AzureOperation.Type.SERVICE
        )
        public SpringCloudDeployment commit() {
            this.deployment.status(Status.PENDING);
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start creating deployment({0})...", this.deployment.name()));
            this.deployment.refresh(this.modifier.create());
            messager.success(AzureString.format("Deployment({0}) is successfully created", this.deployment.name()));
            final SpringCloudDeployment deployment = this.scale(this.newScaleSettings);
            this.deployment.app.refresh();
            return deployment;
        }
    }
}
