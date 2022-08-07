/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.NameAvailability;
import com.azure.resourcemanager.mysql.models.NameAvailabilityRequest;
import com.azure.resourcemanager.mysql.models.PerformanceTierProperties;
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
import java.util.stream.Collectors;

@Getter
public class MySqlServiceSubscription extends AbstractAzServiceSubscription<MySqlServiceSubscription, MySqlManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final MySqlServerModule serverModule;

    MySqlServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureMySql service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.serverModule = new MySqlServerModule(this);
    }

    MySqlServiceSubscription(@Nonnull MySqlManager manager, @Nonnull AzureMySql service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, MySqlServiceSubscription, ?>> getSubModules() {
        return Collections.singletonList(serverModule);
    }

    @Nonnull
    public MySqlServerModule servers() {
        return this.serverModule;
    }

    @Nonnull
    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.serverModule.getName());
    }

    @Nonnull
    public Availability checkNameAvailability(@Nonnull String name) {
        final NameAvailabilityRequest request = new NameAvailabilityRequest().withName(name).withType(this.getParent().getName());
        final NameAvailability result = Objects.requireNonNull(this.getRemote()).checkNameAvailabilities().execute(request);
        return new Availability(result.nameAvailable(), result.reason(), result.message());
    }

    public boolean checkRegionAvailability(@Nonnull Region region) {
        List<PerformanceTierProperties> tiers = Objects.requireNonNull(this.getRemote()).locationBasedPerformanceTiers()
            .list(region.getName()).stream().collect(Collectors.toList());
        return tiers.stream().anyMatch(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()));
    }
}

