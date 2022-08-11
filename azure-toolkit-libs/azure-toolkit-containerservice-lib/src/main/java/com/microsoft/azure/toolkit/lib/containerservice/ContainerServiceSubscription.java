/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerservice;

import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class ContainerServiceSubscription extends AbstractAzServiceSubscription<ContainerServiceSubscription, ContainerServiceManager> {
    @Nonnull
    private final String subscriptionId;
    private final KubernetesClusterModule kubernetesClusterModule;

    protected ContainerServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureContainerService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.kubernetesClusterModule = new KubernetesClusterModule(this);
    }

    protected ContainerServiceSubscription(@Nonnull ContainerServiceManager manager, @Nonnull AzureContainerService service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    public KubernetesClusterModule kubernetes() {
        return this.kubernetesClusterModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ContainerServiceSubscription, ?>> getSubModules() {
        return Collections.singletonList(kubernetesClusterModule);
    }
}
