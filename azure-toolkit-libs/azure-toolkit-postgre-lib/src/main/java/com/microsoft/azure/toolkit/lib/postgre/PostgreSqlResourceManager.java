/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.NameAvailability;
import com.azure.resourcemanager.postgresql.models.NameAvailabilityRequest;
import com.azure.resourcemanager.postgresql.models.PerformanceTierProperties;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
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
public class PostgreSqlResourceManager extends AbstractAzResourceManager<PostgreSqlResourceManager, PostgreSqlManager> {
    @Nonnull
    private final String subscriptionId;
    private final PostgreSqlServerModule serverModule;

    PostgreSqlResourceManager(@Nonnull String subscriptionId, AzurePostgreSql service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.serverModule = new PostgreSqlServerModule(this);
    }

    PostgreSqlResourceManager(PostgreSqlManager manager, AzurePostgreSql service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, PostgreSqlResourceManager, ?>> getSubModules() {
        return Collections.singletonList(serverModule);
    }

    public PostgreSqlServerModule servers() {
        return this.serverModule;
    }

    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.serverModule.getName());
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

