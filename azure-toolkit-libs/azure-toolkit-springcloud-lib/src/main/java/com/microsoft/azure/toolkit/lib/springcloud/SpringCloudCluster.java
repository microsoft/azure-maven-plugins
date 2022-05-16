/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.ProvisioningState;
import com.azure.resourcemanager.appplatform.models.Sku;
import com.azure.resourcemanager.appplatform.models.SkuName;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.TestKeys;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class SpringCloudCluster extends AbstractAzResource<SpringCloudCluster, SpringCloudServiceSubscription, SpringService> {

    @Nonnull
    private final SpringCloudAppModule appModule;

    protected SpringCloudCluster(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull SpringCloudClusterModule module) {
        super(name, resourceGroup, module);
        this.appModule = new SpringCloudAppModule(this);
    }

    /**
     * copy constructor
     */
    protected SpringCloudCluster(@Nonnull SpringCloudCluster origin) {
        super(origin);
        this.appModule = origin.appModule;
    }

    protected SpringCloudCluster(@Nonnull SpringService remote, @Nonnull SpringCloudClusterModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.appModule = new SpringCloudAppModule(this);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringService remote) {
        final ProvisioningState state = remote.refresh().innerModel().properties().provisioningState();
        return state.toString();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, SpringCloudCluster, ?>> getSubModules() {
        return Collections.singletonList(appModule);
    }

    @Nonnull
    public SpringCloudAppModule apps() {
        return appModule;
    }

    @Nullable
    public String getTestEndpoint() {
        return Optional.ofNullable(this.getRemote()).map(SpringService::listTestKeys).map(TestKeys::primaryTestEndpoint).orElse(null);
    }

    @Nullable
    public String getTestKey() {
        return Optional.ofNullable(this.getRemote()).map(SpringService::listTestKeys).map(TestKeys::primaryKey).orElse(null);
    }

    @Nonnull
    public String getSku() {
        return Optional.ofNullable(this.getRemote()).map(SpringService::sku).map(Sku::name).orElse(SkuName.B0.toString());
    }

    public boolean isEnterpriseTier() {
        return this.remoteOptional().map(SpringService::sku).filter(s -> s.name().equalsIgnoreCase(SkuName.E0.toString())).isPresent();
    }
}
