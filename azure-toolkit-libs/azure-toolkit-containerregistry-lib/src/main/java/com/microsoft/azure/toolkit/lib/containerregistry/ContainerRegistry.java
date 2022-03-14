/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.resourcemanager.containerregistry.fluent.models.RegistryInner;
import com.azure.resourcemanager.containerregistry.models.AccessKeyType;
import com.azure.resourcemanager.containerregistry.models.PublicNetworkAccess;
import com.azure.resourcemanager.containerregistry.models.Registry;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ContainerRegistry extends AbstractAzResource<ContainerRegistry, AzureContainerRegistryResourceManager, Registry> {
    protected ContainerRegistry(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AzureContainerRegistryModule module) {
        super(name, resourceGroupName, module);
    }

    protected ContainerRegistry(@Nonnull ContainerRegistry registry) {
        super(registry);
    }

    protected ContainerRegistry(@Nonnull Registry registry, @Nonnull AzureContainerRegistryModule module) {
        super(registry.name(), registry.resourceGroupName(), module);
    }

    @Override
    public List<AzResourceModule<?, ContainerRegistry, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Registry remote) {
        return Optional.ofNullable(remote.innerModel()).map(RegistryInner::status).map(s -> s.displayStatus()).orElse(Status.UNKNOWN);
    }

    public Boolean isAdminUserEnabled() {
        return Optional.ofNullable(getRemote()).map(Registry::adminUserEnabled).orElse(Boolean.FALSE);
    }

    public Boolean isPublicAccessEnabled() {
        return Optional.ofNullable(getRemote()).map(r -> r.publicNetworkAccess() == PublicNetworkAccess.ENABLED).orElse(Boolean.FALSE);
    }

    @Nullable
    public Sku getSku() {
        return Optional.ofNullable(getRemote()).map(Registry::sku).map(sku -> sku.tier().toString()).map(Sku::valueOf).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(registry -> registry.region().name()).map(Region::fromName).orElse(null);
    }

    @Nullable
    public String getUserName() {
        return Optional.ofNullable(getRemote()).map(registry -> registry.getCredentials().username()).orElse(null);
    }

    @Nullable
    public String getPrimaryCredential() {
        return Optional.ofNullable(getRemote()).map(registry -> registry.getCredentials().accessKeys())
                .map(map -> map.get(AccessKeyType.PRIMARY)).orElse(null);
    }

    @Nullable
    public String getSecondaryCredential() {
        return Optional.ofNullable(getRemote()).map(registry -> registry.getCredentials().accessKeys())
                .map(map -> map.get(AccessKeyType.SECONDARY)).orElse(null);
    }

    @Nullable
    public String getLoginServerUrl() {
        return Optional.ofNullable(getRemote()).map(Registry::loginServerUrl).orElse(null);
    }

    @Nullable
    public String getType() {
        return Optional.ofNullable(getRemote()).map(Registry::type).orElse(null);
    }
}
