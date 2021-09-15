/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.ip;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureModule;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.compute.AbstractAzureResource;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicIpAddress extends AbstractAzureResource<com.azure.resourcemanager.network.models.PublicIpAddress, IAzureBaseResource>
        implements AzureOperationEvent.Source<PublicIpAddress> {

    protected AzurePublicIpAddress module;

    public PublicIpAddress(@Nonnull final String id, final AzurePublicIpAddress azureClient) {
        super(id);
        this.module = azureClient;
    }

    public PublicIpAddress(@Nonnull final com.azure.resourcemanager.network.models.PublicIpAddress resource, final AzurePublicIpAddress module) {
        super(resource);
        this.module = module;
    }

    @Override
    public IAzureModule<? extends AbstractAzureResource<com.azure.resourcemanager.network.models.PublicIpAddress, IAzureBaseResource>, ? extends IAzureBaseResource> module() {
        return module;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.PublicIpAddress loadRemote() {
        return module.getPublicIpAddressManager(getSubscriptionId()).getByResourceGroup(resourceGroup(), name());
    }
}
