/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.azure.core.management.Region;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.CapabilityStatus;
import com.azure.resourcemanager.sql.models.CheckNameAvailabilityResult;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerManagerFactory;
import com.microsoft.azure.toolkit.lib.sqlserver.model.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.impl.SqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureSqlServer extends SubscriptionScoped<AzureSqlServer> implements AzureService {

    public AzureSqlServer() {
        super(AzureSqlServer::new);
    }

    private AzureSqlServer(@Nonnull final List<Subscription> subscriptions) {
        super(AzureSqlServer::new, subscriptions);
    }

    public ISqlServer sqlServer(String id) {
        final SqlServerEntity entity = SqlServerEntity.builder().id(id).build();
        return sqlServer(entity);
    }

    public ISqlServer sqlServer(String subscriptionId, String resourceGroup, String name) {
        final SqlServerEntity entity = SqlServerEntity.builder().subscriptionId(subscriptionId).resourceGroup(resourceGroup).name(name).build();
        return sqlServer(entity);
    }

    public ISqlServer sqlServer(SqlServerEntity entity) {
        final String subscriptionId = getSubscriptionFromResourceEntity(entity);
        return new SqlServer(entity, SqlServerManagerFactory.create(subscriptionId));
    }

    private ISqlServer sqlServer(com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        return new SqlServer(sqlServerInner, SqlServerManagerFactory.create(sqlServerInner.manager().subscriptionId()));
    }

    public List<ISqlServer> sqlServers() {
        return getSubscriptions().stream()
                .map(subscription -> SqlServerManagerFactory.create(subscription.getId()))
                .flatMap(manager -> manager.sqlServers().list().stream())
                .collect(Collectors.toList()).stream()
                .map(server -> sqlServer(server))
                .collect(Collectors.toList());
    }

    public CheckNameAvailabilityResultEntity checkNameAvailability(String subscriptionId, String name) {
        SqlServerManager manager = SqlServerManagerFactory.create(subscriptionId);
        CheckNameAvailabilityResult result = manager.sqlServers().checkNameAvailability(name);
        return new CheckNameAvailabilityResultEntity(result.isAvailable(), result.unavailabilityReason(), result.unavailabilityMessage());
    }

    public boolean checkRegionCapability(String subscriptionId, String region) {
        SqlServerManager manager = SqlServerManagerFactory.create(subscriptionId);
        com.azure.resourcemanager.sql.models.RegionCapabilities capabilities = manager.sqlServers().getCapabilitiesByRegion(Region.fromName(region));
        return Objects.nonNull(capabilities.status()) && CapabilityStatus.AVAILABLE == capabilities.status();
    }

    private String getSubscriptionFromResourceEntity(@Nonnull IAzureResourceEntity resourceEntity) {
        if (StringUtils.isNotEmpty(resourceEntity.getId())) {
            return ResourceId.fromString(resourceEntity.getId()).subscriptionId();
        }
        if (StringUtils.isNotEmpty(resourceEntity.getSubscriptionId())) {
            return resourceEntity.getSubscriptionId();
        }
        throw new AzureToolkitRuntimeException("Subscription id is required for this request.");
    }
}
