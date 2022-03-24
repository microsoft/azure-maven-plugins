/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.resourcemanager.containerregistry.ContainerRegistryManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class AzureContainerRegistryResourceManager extends AbstractAzResourceManager<AzureContainerRegistryResourceManager, ContainerRegistryManager> {

    @Nonnull
    private final String subscriptionId;
    private final AzureContainerRegistryModule azureContainerRegistryModule;

    protected AzureContainerRegistryResourceManager(@Nonnull String subscriptionId, @Nonnull AzureContainerRegistry service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.azureContainerRegistryModule = new AzureContainerRegistryModule(this);
    }

    protected AzureContainerRegistryResourceManager(@Nonnull ContainerRegistryManager manager, AzureContainerRegistry service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    public AzureContainerRegistryModule registry() {
        return this.azureContainerRegistryModule;
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, AzureContainerRegistryResourceManager, ?>> getSubModules() {
        return Collections.singletonList(azureContainerRegistryModule);
    }
}
