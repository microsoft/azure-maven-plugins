/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.CapabilityStatus;
import com.azure.resourcemanager.sql.models.CheckNameAvailabilityResult;
import com.azure.resourcemanager.sql.models.RegionCapabilities;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class MicrosoftSqlResourceManager extends AbstractAzResourceManager<MicrosoftSqlResourceManager, SqlServerManager> {
    @Nonnull
    private final String subscriptionId;
    private final MicrosoftSqlServerModule serverModule;

    MicrosoftSqlResourceManager(@Nonnull String subscriptionId, AzureSqlServer service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.serverModule = new MicrosoftSqlServerModule(this);
    }

    MicrosoftSqlResourceManager(SqlServerManager manager, AzureSqlServer service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    public List<AzResourceModule<?, MicrosoftSqlResourceManager, ?>> getSubModules() {
        return Collections.singletonList(serverModule);
    }

    public MicrosoftSqlServerModule servers() {
        return this.serverModule;
    }

    public List<Region> listSupportedRegions() {
        return super.listSupportedRegions(this.serverModule.getName());
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(@Nonnull String name) {
        CheckNameAvailabilityResult result = Objects.requireNonNull(this.getRemote()).sqlServers().checkNameAvailability(name);
        return new CheckNameAvailabilityResultEntity(result.isAvailable(), result.unavailabilityReason(), result.unavailabilityMessage());
    }

    public boolean checkRegionAvailability(Region region) {
        RegionCapabilities capabilities = Objects.requireNonNull(this.getRemote()).sqlServers()
            .getCapabilitiesByRegion(com.azure.core.management.Region.fromName(region.getName()));
        return Objects.nonNull(capabilities.status()) && CapabilityStatus.AVAILABLE == capabilities.status();
    }
}

