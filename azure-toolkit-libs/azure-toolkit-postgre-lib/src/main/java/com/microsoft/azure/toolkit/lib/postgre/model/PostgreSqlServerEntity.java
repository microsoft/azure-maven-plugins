/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre.model;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.Sku;
import com.azure.resourcemanager.postgresql.models.SslEnforcementEnum;
import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServerEntity;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

public class PostgreSqlServerEntity extends AbstractAzureResource.RemoteAwareResourceEntity<Server> implements IDatabaseServerEntity {

    @Nonnull
    private final PostgreSqlManager manager;
    @Nonnull
    private final ResourceId resourceId;

    public PostgreSqlServerEntity(@Nonnull PostgreSqlManager manager, @Nonnull Server server) {
        this.resourceId = ResourceId.fromString(server.id());
        this.remote = server;
        this.manager = manager;
    }

    @Override
    public String getId() {
        return resourceId.id();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    @Override
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

    public String getAdministratorLoginName() {
        return remoteOptional().map(Server::administratorLogin).orElse(null);
    }

    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(Server::fullyQualifiedDomainName).orElse(null);
    }

    @Override
    public boolean isEnableAccessFromAzureServices() {
        return remoteOptional().map(remote -> manager.firewallRules().listByServer(this.getResourceGroupName(), remote.name()).stream()
                .anyMatch(e -> FirewallRuleEntity.ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME.equalsIgnoreCase(e.name()))).orElse(false);
    }

    @Override
    public boolean isEnableAccessFromLocalMachine() {
        return remoteOptional().map(remote -> manager.firewallRules().listByServer(this.getResourceGroupName(), remote.name()).stream()
                .anyMatch(e -> StringUtils.equalsIgnoreCase(FirewallRuleEntity.getAccessFromLocalFirewallRuleName(), e.name()))).orElse(false);
    }

    public String getVersion() {
        return remoteOptional().map(Server::version).map(ExpandableStringEnum::toString).orElse(null);
    }

    public String getState() {
        return remoteOptional().map(Server::userVisibleState).map(ExpandableStringEnum::toString).orElse(null);
    }

    private Optional<Server> remoteOptional() {
        return Optional.ofNullable(this.remote);
    }

    public String getType() {
        return remoteOptional().map(Server::type).orElse(null);
    }

    public String getSkuTier() {
        return remoteOptional().map(Server::sku).map(Sku::tier).map(ExpandableStringEnum::toString).orElse(null);
    }

    public int getVCore() {
        return remoteOptional().map(Server::sku).map(Sku::capacity).orElse(0);
    }

    public int getStorageInMB() {
        return remoteOptional().map(Server::storageProfile).map(StorageProfile::storageMB).orElse(0);
    }

    public String getSslEnforceStatus() {
        return remoteOptional().map(Server::sslEnforcement).map(SslEnforcementEnum::name).orElse(null);
    }
}
