/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.CheckNameAvailabilityRequest;
import com.azure.resourcemanager.postgresqlflexibleserver.models.NameAvailability;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class PostgreSqlServiceSubscription extends AbstractAzServiceSubscription<PostgreSqlServiceSubscription, PostgreSqlManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final PostgreSqlServerModule serverModule;

    PostgreSqlServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzurePostgreSql service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.serverModule = new PostgreSqlServerModule(this);
    }

    PostgreSqlServiceSubscription(@Nonnull PostgreSqlManager manager, @Nonnull AzurePostgreSql service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(serverModule);
    }

    @Nonnull
    public PostgreSqlServerModule servers() {
        return this.serverModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.serverModule.getName());
    }

    @Nonnull
    public Availability checkNameAvailability(@Nonnull String location, @Nonnull String name) {
        final CheckNameAvailabilityRequest request = new CheckNameAvailabilityRequest().withName(name).withType("Microsoft.DBforPostgreSQL/flexibleServers");
        final NameAvailability result = Objects.requireNonNull(this.getRemote()).checkNameAvailabilityWithLocations().execute(location, request);
        return new Availability(result.nameAvailable(), Optional.ofNullable(result.reason()).map(ExpandableStringEnum::toString).orElse(""), result.message());
    }

    public boolean checkRegionAvailability(@Nonnull Region region) {
        return Objects.requireNonNull(this.getRemote()).locationBasedCapabilities()
            .execute(region.getName()).stream()
            .anyMatch(e -> CollectionUtils.isNotEmpty(e.supportedFlexibleServerEditions()));
    }
}

