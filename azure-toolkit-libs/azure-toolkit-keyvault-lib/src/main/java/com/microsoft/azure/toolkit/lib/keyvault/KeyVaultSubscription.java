/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvault;

import com.azure.resourcemanager.keyvault.KeyVaultManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

@Getter
public class KeyVaultSubscription extends AbstractAzServiceSubscription<KeyVaultSubscription, KeyVaultManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final KeyVaultModule keyVaultModule;

    protected KeyVaultSubscription(@Nonnull String subscriptionId, @Nonnull AzureKeyVault service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.keyVaultModule = new KeyVaultModule(this);
    }

    protected KeyVaultSubscription(@Nonnull KeyVaultManager manager, @Nonnull AzureKeyVault service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    public KeyVaultModule eventHubsNamespaces() {
        return this.keyVaultModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(keyVaultModule);
    }

    @Override
    public List<Region> listSupportedRegions(@Nonnull String resourceType) {
        return super.listSupportedRegions(this.keyVaultModule.getName());
    }
}


