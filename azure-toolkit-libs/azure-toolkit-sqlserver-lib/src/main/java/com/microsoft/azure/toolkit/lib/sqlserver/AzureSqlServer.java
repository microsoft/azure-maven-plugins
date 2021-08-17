/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.core.management.Region;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.CapabilityStatus;
import com.azure.resourcemanager.sql.models.CheckNameAvailabilityResult;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.SubscriptionScoped;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerConfig;
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

    public SqlServer sqlServer(String id) {
        final com.azure.resourcemanager.sql.models.SqlServer remote =
                SqlServerManagerFactory.create(ResourceId.fromString(id).subscriptionId()).sqlServers().getById(id);
        return new SqlServer(remote.manager(), remote);
    }

    public SqlServer sqlServer(String subscriptionId, String resourceGroup, String name) {
        final com.azure.resourcemanager.sql.models.SqlServer remote =
                SqlServerManagerFactory.create(subscriptionId).sqlServers().getByResourceGroup(resourceGroup, name);
        return new SqlServer(remote.manager(), remote);
    }

    public ICommittable<SqlServer> create(SqlServerConfig config) {
        return new Creator(config);
    }

    public List<SqlServer> list() {
        return getSubscriptions().stream()
                .map(subscription -> SqlServerManagerFactory.create(subscription.getId()))
                .flatMap(manager -> manager.sqlServers().list().stream())
                .collect(Collectors.toList()).stream()
                .map(remote -> new SqlServer(remote.manager(), remote))
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

    class Creator implements ICommittable<SqlServer>, AzureOperationEvent.Source<SqlServerConfig> {

        private SqlServerConfig config;

        Creator(SqlServerConfig config) {
            this.config = config;
        }

        @Override
        @AzureOperation(name = "sqlserver|server.create", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
        public SqlServer commit() {
            // create
            com.azure.resourcemanager.sql.models.SqlServer remote = SqlServerManagerFactory.create(config.getSubscriptionId()).sqlServers()
                    .define(config.getName())
                    .withRegion(config.getRegion().getName())
                    .withExistingResourceGroup(config.getResourceGroupName())
                    .withAdministratorLogin(config.getAdministratorLoginName())
                    .withAdministratorPassword(config.getAdministratorLoginPassword())
                    .create();
            SqlServer server = new SqlServer(remote.manager(), remote);
            // update firewall rules
            server.update()
                    .withEnableAccessFromAzureServices(config.isEnableAccessFromAzureServices())
                    .withEnableAccessFromLocalMachine(config.isEnableAccessFromLocalMachine())
                    .commit();
            return server;
        }

        public AzureOperationEvent.Source<SqlServerConfig> getEventSource() {
            return new AzureOperationEvent.Source<SqlServerConfig>() {};
        }
    }

}
