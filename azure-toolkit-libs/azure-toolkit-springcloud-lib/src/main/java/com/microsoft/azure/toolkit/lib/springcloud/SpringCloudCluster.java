/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.Region;
import com.azure.resourcemanager.appplatform.fluent.models.ServiceResourceInner;
import com.azure.resourcemanager.appplatform.models.ClusterResourceProperties;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.TestKeys;
import com.azure.resourcemanager.resources.fluentcore.arm.models.Resource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class SpringCloudCluster extends AbstractAzResource<SpringCloudCluster, SpringCloudServiceSubscription, SpringService>
    implements Deletable {

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
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull SpringService remote) {
        return remote.innerModel().properties().provisioningState().toString();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
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

    @Nullable
    public Region getRegion() {
        return this.remoteOptional().map(Resource::region).orElse(null);
    }

    @Nonnull
    public Sku getSku() {
        return Optional.ofNullable(this.getRemote()).map(SpringService::sku)
            .map(Sku::fromSku).orElse(Sku.UNKNOWN);
    }

    @Nullable
    public String getManagedEnvironmentId() {
        return Optional.ofNullable(this.getRemote()).map(SpringService::innerModel)
            .map(ServiceResourceInner::properties)
            .map(ClusterResourceProperties::managedEnvironmentId).orElse(null);
    }

    public boolean isEnterpriseTier() {
        return getSku().isEnterpriseTier();
    }

    public boolean isStandardTier() {
        return getSku().isStandardTier();
    }

    public boolean isBasicTier() {
        return getSku().isBasicTier();
    }

    public boolean isConsumptionTier() {
        return getSku().isConsumptionTier();
    }

    @Nonnull
    public String getFqdn() {
        return this.remoteOptional().map(SpringService::innerModel).map(ServiceResourceInner::properties).map(ClusterResourceProperties::fqdn).orElse("");
    }
}
