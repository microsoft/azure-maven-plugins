/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cognitiveservices;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.cognitiveservices.CognitiveServicesManager;
import com.azure.resourcemanager.cognitiveservices.models.Deployment;
import com.azure.resourcemanager.cognitiveservices.models.Deployments;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class CognitiveDeploymentModule extends
    AbstractAzResourceModule<CognitiveDeployment, CognitiveAccount, Deployment> {
    public static final String NAME = "deployments";

    public CognitiveDeploymentModule(@Nonnull CognitiveAccount parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected CognitiveDeployment newResource(@Nonnull Deployment deployment) {
        return new CognitiveDeployment(deployment, this);
    }

    @Nonnull
    @Override
    protected CognitiveDeployment newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new CognitiveDeployment(name, resourceGroupName, this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Deployment>> loadResourcePagesFromAzure() {
        final CognitiveAccount account = getParent();
        return Optional.ofNullable(this.getClient())
            .map(c -> c.list(account.getResourceGroupName(), account.getName()).iterableByPage(getPageSize()).iterator()).orElse(Collections.emptyIterator());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/openai.load_deployment.deployment", params = {"name"})
    protected Deployment loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final CognitiveAccount account = getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.get(resourceGroup, account.getName(), name)).orElse(null);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<CognitiveDeployment, Deployment> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        assert rgName != null : "'Resource group' is required.";
        return new CognitiveDeploymentDraft(name, rgName, this);
    }

    @Override
    @AzureOperation(name = "azure/openai.delete_deployment.deployment", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        Optional.ofNullable(getClient()).ifPresent(client -> client.delete(id.resourceGroupName(), id.parent().name(), id.name()));
    }

    @Nullable
    @Override
    protected Deployments getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote())
            .map(CognitiveServicesManager::deployments).orElse(null);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Azure OpenAI deployment";
    }
}
