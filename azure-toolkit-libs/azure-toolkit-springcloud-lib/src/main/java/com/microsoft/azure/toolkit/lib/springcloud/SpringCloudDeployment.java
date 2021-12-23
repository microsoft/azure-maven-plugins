/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.azure.resourcemanager.appplatform.models.DeploymentResourceStatus;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.google.common.base.Charsets;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import io.netty.handler.codec.http.HttpHeaders;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class SpringCloudDeployment extends AbstractAzResource<SpringCloudDeployment, SpringCloudApp, SpringAppDeployment> {

    protected SpringCloudDeployment(@Nonnull String name, @Nonnull SpringCloudDeploymentModule module) {
        super(name, module.getParent().getResourceGroup(), module);
    }

    protected SpringCloudDeployment(@Nonnull SpringAppDeployment remote, @Nonnull SpringCloudDeploymentModule module) {
        this(remote.name(), module);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringAppDeployment remote) {
        return Optional.of(remote)
            .map(SpringAppDeployment::status)
            .orElse(DeploymentResourceStatus.UNKNOWN).toString();
    }

    @Override
    public List<AzResourceModule<?, SpringCloudDeployment, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Override
    public String formalizeStatus(String status) {
        switch (status.toUpperCase()) {
            case "ALLOCATING":
            case "UPGRADING":
            case "COMPILING":
                return Status.PENDING;
            case "RUNNING":
                return Status.RUNNING;
            case "STOPPED":
                return Status.STOPPED;
            case "FAILED":
                return Status.ERROR;
            default:
                return Status.UNKNOWN;
        }
    }

    @SneakyThrows
    public Flux<String> streamLogs(final String instance) {
        return streamLogs(instance, 0, 10, 0, true);
    }

    @SneakyThrows
    public Flux<String> streamLogs(final String instance, int sinceSeconds, int tailLines, int limitBytes, boolean follow) {
        final HttpClient client = HttpClient.create().keepAlive(true);
        final URIBuilder endpoint = new URIBuilder(this.getParent().getLogStreamingEndpoint(instance));
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
        final String password = this.getParent().getParent().getTestKey();
        final String userPass = "primary:" + password;
        final String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
        final Consumer<? super HttpHeaders> headerBuilder = header -> header.set("Authorization", basicAuth);
        return client.headers(headerBuilder)
            .responseTimeout(Duration.of(10, ChronoUnit.MINUTES))
            .get()
            .uri(endpoint.build())
            .response((resp, cont) -> resp.status().code() == 200 ? cont.asString(Charsets.UTF_8) : Mono.empty());
    }

    @AzureOperation(
        name = "springcloud.wait_until_deployment_ready.deployment|app",
        params = {"this.entity().getName()", "this.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public boolean waitUntilReady(int timeoutInSeconds) {
        AzureMessager.getMessager().info("Getting deployment status...");
        final SpringCloudDeployment deployment = Utils.pollUntil(() -> {
            this.refresh();
            return this;
        }, Utils::isDeploymentDone, timeoutInSeconds);
        return Utils.isDeploymentDone(deployment);
    }

    @Nonnull
    public Integer getCpu() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::cpu)
            .orElse(1);
    }

    @Nonnull
    public Integer getMemoryInGB() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::memoryInGB)
            .orElse(1);
    }

    @Nonnull
    public String getRuntimeVersion() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(s -> s.runtimeVersion().toString())
            .orElse(SpringCloudJavaVersion.JAVA_8);
    }

    @Nullable
    public String getJvmOptions() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::jvmOptions)
            .orElse(null);
    }

    @Nullable
    public Map<String, String> getEnvironmentVariables() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::environmentVariables)
            .orElse(null);
    }

    @Nonnull
    public List<DeploymentInstance> getInstances() {
        if (Objects.nonNull(this.getRemote())) {
            return this.getRemote().instances();
        }
        return new ArrayList<>();
    }

    @Nonnull
    public Boolean isActive() {
        return Optional.ofNullable(this.getRemote()).map(SpringAppDeployment::isActive).orElse(false);
    }
}
