/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServerEntity;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

public class SqlServerEntity extends AbstractAzureEntityManager.RemoteAwareResourceEntity<SqlServer> implements IDatabaseServerEntity {

    @Nonnull
    private final ResourceId resourceId;

    public SqlServerEntity(@Nonnull SqlServer server) {
        this.resourceId = ResourceId.fromString(server.id());
        this.remote = server;
    }

    public String getId() {
        return resourceId.id();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    public String getResourceGroupName() {
        return resourceId.resourceGroupName();
    }

    @Override
    public String getSubscriptionId() {
        return resourceId.subscriptionId();
    }

    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Override
    public String getAdministratorLoginName() {
        return remoteOptional().map(SqlServer::administratorLogin).orElse(null);
    }

    @Override
    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(SqlServer::fullyQualifiedDomainName).orElse(null);
    }

    @Override
    public boolean isEnableAccessFromAzureServices() {
        return remoteOptional().map(remote -> remote.firewallRules().list().stream()
                .anyMatch(e -> FirewallRuleEntity.ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME.equalsIgnoreCase(e.name()))).orElse(false);
    }

    @Override
    public boolean isEnableAccessFromLocalMachine() {
        return remoteOptional().map(remote -> remote.firewallRules().list().stream()
                .anyMatch(e -> StringUtils.equalsIgnoreCase(FirewallRuleEntity.getAccessFromLocalFirewallRuleName(), e.name()))).orElse(false);
    }

    @Override
    public String getState() {
        return remoteOptional().map(SqlServer::state).orElse(null);
    }

    private Optional<SqlServer> remoteOptional() {
        return Optional.ofNullable(this.remote);
    }
}
