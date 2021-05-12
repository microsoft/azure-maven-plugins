/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.models.CheckNameAvailabilityResult;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.auth.AzureResourceManagerFactory;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.impl.SqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
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
        return new SqlServer(entity, AzureResourceManagerFactory.produce(subscriptionId));
    }

    private ISqlServer sqlServer(com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        return new SqlServer(sqlServerInner, AzureResourceManagerFactory.produce(sqlServerInner.manager().subscriptionId()));
    }

    public List<ISqlServer> sqlServers() {
        return getSubscriptions().stream()
                .map(subscription -> AzureResourceManagerFactory.produce(subscription.getId()))
                .flatMap(azureResourceManager -> azureResourceManager.sqlServers().list().stream())
                .collect(Collectors.toList()).stream()
                .map(server -> sqlServer(server))
                .collect(Collectors.toList());
    }

    public boolean checkNameAvailability(String subscriptionId, String name) {
        AzureResourceManager resourceManager = AzureResourceManagerFactory.produce(subscriptionId);
        CheckNameAvailabilityResult nameAvailabilityResult = resourceManager.sqlServers().checkNameAvailability(name);
        return nameAvailabilityResult.isAvailable();
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
