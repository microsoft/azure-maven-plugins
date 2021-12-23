/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class SpringCloudDeploymentDraft extends SpringCloudDeployment implements AzResource.Draft<SpringCloudDeployment, SpringAppDeployment> {

    @Nullable
    private SpringCloudDeploymentConfig config;

    protected SpringCloudDeploymentDraft(@Nonnull String name, @Nonnull SpringCloudDeploymentModule module) {
        super(name, module);
        this.setStatus(Status.DRAFT);
    }

    @AzureOperation(
        name = "springcloud.create_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringAppDeployment createResourceInAzure() {
        final String name = this.getName();
        final SpringApp app = Objects.requireNonNull(this.getParent().getRemote());
        final SpringAppDeploymentImpl create = ((SpringAppDeploymentImpl) app.deployments().define(name));
        modify(create, this.config);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating deployment({0})...", name));
        final SpringAppDeployment deployment = create.create();
        messager.success(AzureString.format("Deployment({0}) is successfully created", name));
        if (Objects.nonNull(config)) {
            return this.scaleDeploymentInAzure(deployment, config.getScaleSettings());
        }
        return deployment;
    }

    @Override
    @AzureOperation(
        name = "springcloud.update_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringAppDeployment updateResourceInAzure(@Nonnull SpringAppDeployment origin) {
        final SpringAppDeploymentImpl update = ((SpringAppDeploymentImpl) Objects.requireNonNull(origin).update());
        if (modify(update, config)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating deployment({0})...", origin.name()));
            origin = update.apply();
            messager.success(AzureString.format("Deployment({0}) is successfully updated", origin.name()));
        }
        if (Objects.nonNull(config)) {
            return this.scaleDeploymentInAzure(origin, config.getScaleSettings());
        }
        return origin;
    }

    @AzureOperation(
        name = "springcloud.scale_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    SpringAppDeployment scaleDeploymentInAzure(SpringAppDeployment deployment, ScaleSettings newScale) {
        final Optional<DeploymentSettings> temp = Optional.ofNullable(deployment.settings());
        final ScaleSettings oldScale = ScaleSettings.builder()
            .cpu(temp.map(DeploymentSettings::cpu).orElse(null))
            .memoryInGB(temp.map(DeploymentSettings::memoryInGB).orElse(null))
            .capacity(Optional.ofNullable(deployment.innerModel().sku()).map(Sku::capacity).orElse(null))
            .build();
        if (Objects.equals(newScale, oldScale)) {
            return deployment;
        }
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start scaling deployment({0})...", deployment.name()));
        final SpringAppDeployment result = deployment.update()
            .withCpu(newScale.getCpu()).withMemory(newScale.getMemoryInGB()).withInstance(newScale.getCapacity()).apply();
        messager.success(AzureString.format("Deployment({0}) is successfully scaled.", deployment.name()));
        return result;
    }

    boolean modify(@Nonnull SpringAppDeploymentImpl deployment, @Nullable SpringCloudDeploymentConfig config) {
        if (Objects.isNull(config)) {
            return false;
        }
        boolean skippable = true;
        final Map<String, String> oldEnv = Optional.ofNullable(deployment.settings()).map(DeploymentSettings::environmentVariables).orElse(null);
        final String oldJvmOptions = Optional.ofNullable(deployment.settings()).map(DeploymentSettings::jvmOptions).orElse(null);
        final RuntimeVersion oldRuntimeVersion = Optional.ofNullable(deployment.settings()).map(DeploymentSettings::runtimeVersion).orElse(null);

        final Map<String, String> newEnv = config.getEnvironment();
        final String newJvmOptions = Optional.ofNullable(config.getJvmOptions()).map(String::trim).orElse("");
        final RuntimeVersion newVersion = StringUtils.isBlank(config.getJavaVersion()) ?
            RuntimeVersion.JAVA_8 : RuntimeVersion.fromString(config.getJavaVersion());
        final File newArtifact = Optional.ofNullable(config.getArtifact()).map(IArtifact::getFile).orElse(null);

        final boolean allEmpty = MapUtils.isEmpty(newEnv) && MapUtils.isEmpty(oldEnv);
        if (!allEmpty && !Objects.equals(newEnv, oldEnv) && Objects.nonNull(newEnv)) {
            skippable = false;
            newEnv.forEach((key, value) -> {
                if (StringUtils.isBlank(value)) {
                    deployment.withoutEnvironment(key);
                } else {
                    deployment.withEnvironment(key, value);
                }
            });
        }
        if (!StringUtils.isAllBlank(newJvmOptions, oldJvmOptions) && !Objects.equals(newJvmOptions, oldJvmOptions)) {
            skippable = false;
            deployment.withJvmOptions(newJvmOptions);
        }
        if (!Objects.equals(oldRuntimeVersion, newVersion)) {
            skippable = false;
            deployment.withRuntime(newVersion);
        }
        if (Objects.nonNull(newArtifact)) {
            skippable = false;
            deployment.withJarFile(newArtifact);
        }
        return !skippable;
    }
}