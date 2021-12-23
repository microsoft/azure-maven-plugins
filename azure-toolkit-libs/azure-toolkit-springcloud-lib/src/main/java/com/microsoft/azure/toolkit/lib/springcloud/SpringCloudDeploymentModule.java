/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RuntimeVersion;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployments;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudDeploymentModule extends AbstractAzResourceModule<SpringCloudDeployment, SpringCloudApp, SpringAppDeployment> {

    public static final String NAME = "deployments";

    public SpringCloudDeploymentModule(@Nonnull SpringCloudApp parent) {
        super(NAME, parent);
    }

    @Override
    public SpringAppDeployments<?> getClient() {
        return this.parent.getRemote().deployments();
    }

    @AzureOperation(
        name = "springcloud.create_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected SpringAppDeployment createResourceInAzure(@Nonnull String name, @Nonnull String resourceGroup, Object cfg) {
        final SpringCloudDeploymentConfig config = (SpringCloudDeploymentConfig) cfg;
        final SpringAppDeploymentImpl create = ((SpringAppDeploymentImpl) this.getClient().define(name));
        modify(create, config);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating deployment({0})...", name));
        final SpringAppDeployment deployment = create.create();
        messager.success(AzureString.format("Deployment({0}) is successfully created", name));
        return this.scaleDeploymentInAzure(deployment, config.getScaleSettings());
    }

    @Override
    @AzureOperation(
        name = "springcloud.update_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected SpringAppDeployment updateResourceInAzure(@Nonnull SpringAppDeployment deployment, Object cfg) {
        final SpringCloudDeploymentConfig config = (SpringCloudDeploymentConfig) cfg;
        final SpringAppDeploymentImpl update = ((SpringAppDeploymentImpl) Objects.requireNonNull(deployment).update());
        if (modify(update, config)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating deployment({0})...", deployment.name()));
            deployment = update.apply();
            messager.success(AzureString.format("Deployment({0}) is successfully updated", deployment.name()));
        }
        return this.scaleDeploymentInAzure(deployment, config.getScaleSettings());
    }

    @AzureOperation(
        name = "springcloud.scale_deployment.deployment|app",
        params = {"this.deployment.name()", "this.deployment.app.name()"},
        type = AzureOperation.Type.SERVICE
    )
    private SpringAppDeployment scaleDeploymentInAzure(SpringAppDeployment deployment, ScaleSettings newScale) {
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

    private boolean modify(SpringAppDeploymentImpl deployment, SpringCloudDeploymentConfig config) {
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

    @Override
    public SpringCloudDeployment newResource(@Nonnull String name, @Nonnull String resourceGroup) {
        return new SpringCloudDeployment(name, this);
    }

    @Nonnull
    protected SpringCloudDeployment newResource(SpringAppDeployment remote) {
        return new SpringCloudDeployment(remote, this);
    }
}
