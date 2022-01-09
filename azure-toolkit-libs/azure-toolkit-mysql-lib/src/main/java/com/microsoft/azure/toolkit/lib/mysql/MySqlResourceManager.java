/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.NameAvailability;
import com.azure.resourcemanager.mysql.models.NameAvailabilityRequest;
import com.azure.resourcemanager.mysql.models.PerformanceTierProperties;
import com.azure.resourcemanager.mysql.models.ServerVersion;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.IResourceManager;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class MySqlResourceManager extends AbstractAzResource<MySqlResourceManager, AzResource.None, MySqlManager>
    implements IResourceManager<MySqlResourceManager, AzResource.None, MySqlManager> {
    @Nonnull
    private final String subscriptionId;
    private final MySqlServerModule serverModule;

    MySqlResourceManager(@Nonnull String subscriptionId, AzureMySql service) {
        super(subscriptionId, AzResource.RESOURCE_GROUP_PLACEHOLDER, service);
        this.subscriptionId = subscriptionId;
        this.serverModule = new MySqlServerModule(this);
    }

    MySqlResourceManager(MySqlManager manager, AzureMySql service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, MySqlResourceManager, ?>> getSubModules() {
        return Collections.singletonList(serverModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull MySqlManager remote) {
        return Status.UNKNOWN;
    }

    public MySqlServerModule servers() {
        return this.serverModule;
    }

    public List<Region> listSupportedRegions() {
        return IResourceManager.super.listSupportedRegions(this.serverModule.getName());
    }

    @Override
    public AzService getService() {
        return (AzureMySql) this.getModule();
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String name) {
        final NameAvailabilityRequest request = new NameAvailabilityRequest().withName(name).withType(this.getParent().getName());
        final NameAvailability result = Objects.requireNonNull(this.getRemote()).checkNameAvailabilities().execute(request);
        return new CheckNameAvailabilityResultEntity(result.nameAvailable(), result.reason(), result.message());
    }

    public boolean checkRegionAvailability(@Nonnull Region region) {
        List<PerformanceTierProperties> tiers = Objects.requireNonNull(this.getRemote()).locationBasedPerformanceTiers()
            .list(region.getName()).stream().collect(Collectors.toList());
        return tiers.stream().anyMatch(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()));
    }
}

