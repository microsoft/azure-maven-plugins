/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.cognitiveservices.models.Deployment;
import com.azure.resourcemanager.cognitiveservices.models.DeploymentProperties;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentSku;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class CognitiveDeploymentDraft extends CognitiveDeployment
    implements AzResource.Draft<CognitiveDeployment, Deployment> {

    @Getter
    @Nullable
    private final CognitiveDeployment origin;

    @Getter
    @Setter
    private Config config;

    protected CognitiveDeploymentDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CognitiveDeploymentModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    protected CognitiveDeploymentDraft(@Nonnull CognitiveDeployment origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    public Deployment createResourceInAzure() {
        final CognitiveAccount account = getParent();
        final CognitiveServicesManager manager = Objects.requireNonNull(account.getParent().getRemote());
        final DeploymentModel model = Objects.requireNonNull(getModel(), "Model is required to create Cognitive deployment.");
        final DeploymentSku sku = Objects.requireNonNull(getSku(), "Sku is required to create Cognitive deployment.");
        final DeploymentProperties deploymentProperties = new DeploymentProperties().withModel(model.toModel());
        AzureMessager.getMessager().info(AzureString.format("Start creating Cognitive deployment({0})...", this.getName()));
        final Deployment deployment = manager.deployments().define(this.getName())
            .withExistingAccount(account.getResourceGroupName(), account.getName())
            .withSku(sku.toSku())
            .withProperties(deploymentProperties)
            .create();
        AzureMessager.getMessager().success(AzureString.format("Cognitive deployment({0}) is successfully created.", this.getName()));
        return deployment;
    }

    @Nonnull
    @Override
    public Deployment updateResourceInAzure(@Nonnull Deployment origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return config != null && Objects.equals(config, new Config());
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Override
    public DeploymentModel getModel() {
        return Optional.ofNullable(config).map(Config::getModel).orElseGet(super::getModel);
    }

    @Override
    public DeploymentSku getSku() {
        return Optional.ofNullable(config).map(Config::getSku).orElseGet(super::getSku);
    }

    @Data
    @EqualsAndHashCode
    public static class Config {
        private DeploymentSku sku;
        private DeploymentModel model;
    }
}
