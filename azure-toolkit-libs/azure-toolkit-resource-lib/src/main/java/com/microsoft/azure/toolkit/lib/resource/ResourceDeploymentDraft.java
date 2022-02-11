/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.resource;

import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class ResourceDeploymentDraft extends ResourceDeployment
    implements AzResource.Draft<ResourceDeployment, com.azure.resourcemanager.resources.models.Deployment> {

    @Getter
    @Nullable
    private final ResourceDeployment origin;
    @Nullable
    private Config config;

    ResourceDeploymentDraft(@Nonnull String name, String resourceGroupName, @Nonnull ResourceDeploymentModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    ResourceDeploymentDraft(@Nonnull ResourceDeployment origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @SneakyThrows({IOException.class})
    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.resources.models.Deployment createResourceInAzure() {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final ResourceGroup group = this.getParent();
        final String name = this.getName();
        final String template = this.getTemplateAsJson();
        final String parameters = this.getParametersAsJson();
        if (StringUtils.isAnyBlank(name, template, parameters)) {
            throw new AzureToolkitRuntimeException("'name', 'template', 'parameters' are all required to create deployment.");
        }
        final ResourceManager manager = Objects.requireNonNull(this.getParent().getParent().getRemote());
        final Deployment.DefinitionStages.Blank define = manager.deployments().define(name);
        final Deployment.DefinitionStages.WithTemplate withTemplate = group.exists() ?
            define.withExistingResourceGroup(group.getName()) :
            define.withNewResourceGroup(group.getName(), com.azure.core.management.Region.fromName(group.getRegion().getName()));
        final Deployment.DefinitionStages.WithCreate definition = withTemplate
            .withTemplate(template)
            .withParameters(parameters)
            .withMode(DeploymentMode.INCREMENTAL);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Deployment({0})...", name));
        final com.azure.resourcemanager.resources.models.Deployment deployment = definition.create();
        messager.success(AzureString.format("Deployment({0}) is successfully created.", name));
        return deployment;
    }

    @SneakyThrows({IOException.class})
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public com.azure.resourcemanager.resources.models.Deployment updateResourceInAzure(@Nonnull com.azure.resourcemanager.resources.models.Deployment origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final String name = this.getName();
        final String oldTemplate = super.getTemplateAsJson();
        final String oldParameters = super.getParametersAsJson();
        final String newTemplate = this.getTemplateAsJson();
        final String newParameters = this.getParametersAsJson();
        final Deployment.Update update = origin.update();
        boolean modified = false;
        if (!StringUtils.equals(newTemplate, oldTemplate) && StringUtils.isNotBlank(newTemplate)) {
            modified = true;
            update.withTemplate(newTemplate);
        }
        if (!StringUtils.equals(newParameters, oldParameters) && StringUtils.isNotBlank(newParameters)) {
            modified = true;
            update.withParameters(newParameters);
        }
        if (modified) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start creating Deployment({0})...", name));
            update.withMode(DeploymentMode.INCREMENTAL);
            origin = update.apply();
            messager.success(AzureString.format("Deployment({0}) is successfully created.", name));
        }
        return origin;
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    public void setTemplateAsJson(@Nonnull String template) {
        this.ensureConfig().setTemplateAsJson(template);
    }

    public void setParametersAsJson(@Nonnull String parameters) {
        this.ensureConfig().setParametersAsJson(parameters);
    }

    @Nullable
    @Override
    public String getTemplateAsJson() {
        return Objects.requireNonNull(Optional.ofNullable(config)
            .map(ResourceDeploymentDraft.Config::getTemplateAsJson)
            .orElseGet(super::getTemplateAsJson));
    }

    @Nullable
    @Override
    public String getParametersAsJson() {
        return Objects.requireNonNull(Optional.ofNullable(config)
            .map(ResourceDeploymentDraft.Config::getParametersAsJson)
            .orElseGet(super::getParametersAsJson));
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getTemplateAsJson()) || Objects.equals(this.config.getTemplateAsJson(), super.getTemplateAsJson()) ||
            Objects.isNull(this.config.getParametersAsJson()) || Objects.equals(this.config.getParametersAsJson(), super.getParametersAsJson());
        return !notModified;
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        private String templateAsJson;
        private String parametersAsJson;
    }
}