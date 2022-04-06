/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.resourcemanager.containerregistry.ContainerRegistryManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class AzureContainerRegistryServiceSubscription extends AbstractAzServiceSubscription<AzureContainerRegistryServiceSubscription, ContainerRegistryManager> {

    @Nonnull
    private final String subscriptionId;
    private final AzureContainerRegistryModule azureContainerRegistryModule;

    protected AzureContainerRegistryServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureContainerRegistry service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.azureContainerRegistryModule = new AzureContainerRegistryModule(this);
    }

    protected AzureContainerRegistryServiceSubscription(@Nonnull ContainerRegistryManager manager, AzureContainerRegistry service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    public AzureContainerRegistryModule registry() {
        return this.azureContainerRegistryModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, AzureContainerRegistryServiceSubscription, ?>> getSubModules() {
        return Collections.singletonList(azureContainerRegistryModule);
    }
}
