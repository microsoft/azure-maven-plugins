/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerCreator;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerUpdater;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SqlServer implements AzureOperationEvent.Source<SqlServer> {
    private SqlServerEntity entity;

    private final SqlServerManager manager;

    private com.azure.resourcemanager.sql.models.SqlServer sqlServerInner;

    public SqlServer(@NotNull SqlServerEntity entity, SqlServerManager manager) {
        this.entity = entity;
        this.manager = manager;
    }

    public SqlServer(com.azure.resourcemanager.sql.models.SqlServer sqlServerInner, SqlServerManager manager) {
        this.sqlServerInner = sqlServerInner;
        this.manager = manager;
        this.entity = this.fromSqlServer(sqlServerInner);
    }

    public SqlServerEntity entity() {
        this.refreshInnerIfNotSet();
        return entity;
    }

    @AzureOperation(name = "sqlserver|server.delete", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (StringUtils.isNotBlank(entity.getId())) {
            SqlServer.this.manager.sqlServers().deleteById(entity.getId());
        } else if (StringUtils.isNotBlank(entity.getResourceGroup()) && StringUtils.isNotBlank(entity.getName())) {
            SqlServer.this.manager.sqlServers().deleteByResourceGroup(entity.getResourceGroup(), entity.getName());
        } else {
            throw new AzureToolkitRuntimeException("Missing necessary parameters to delete SQL Server.");
        }
    }

    public ISqlServerCreator<? extends SqlServer> create() {
        return new SqlServerCreator()
            .withName(entity.getName())
            .withResourceGroup(entity.getResourceGroup())
            .withRegion(entity.getRegion())
            .withAdministratorLogin(entity.getAdministratorLoginName())
            .withEnableAccessFromAzureServices(entity.isEnableAccessFromAzureServices())
            .withEnableAccessFromLocalMachine(entity.isEnableAccessFromLocalMachine());
    }

    public ISqlServerUpdater<? extends SqlServer> update() {
        return new SqlServerUpdater()
            .withEnableAccessFromAzureServices(SqlServer.this.entity.isEnableAccessFromAzureServices())
            .withEnableAccessFromLocalMachine(SqlServer.this.entity.isEnableAccessFromLocalMachine());
    }

    public List<FirewallRuleEntity> firewallRules() {
        this.refreshInnerIfNotSet();
        return sqlServerInner.firewallRules().list().stream().map(this::formSqlServerFirewallRule).collect(Collectors.toList());
    }

    public List<SqlDatabaseEntity> databases() {
        this.refreshInnerIfNotSet();
        return sqlServerInner.databases().list().stream().map(this::formSqlDatabase).collect(Collectors.toList());
    }

    private SqlServerEntity fromSqlServer(com.azure.resourcemanager.sql.models.SqlServer server) {
        return SqlServerEntity.builder().name(server.name())
            .id(server.id())
            .region(Region.fromName(server.regionName()))
            .resourceGroup(server.resourceGroupName())
            .subscriptionId(ResourceId.fromString(server.id()).subscriptionId())
            .kind(server.kind())
            .administratorLoginName(server.administratorLogin())
            .version(server.version())
            .state(server.state())
            .fullyQualifiedDomainName(server.fullyQualifiedDomainName())
            .type(server.type())
            .build();
    }

    private FirewallRuleEntity formSqlServerFirewallRule(com.azure.resourcemanager.sql.models.SqlFirewallRule firewallRuleInner) {
        return FirewallRuleEntity.builder().id(firewallRuleInner.id())
            .name(firewallRuleInner.name())
            .startIpAddress(firewallRuleInner.startIpAddress())
            .endIpAddress(firewallRuleInner.endIpAddress())
            .subscriptionId(firewallRuleInner.sqlServerName())
            .build();
    }

    private SqlDatabaseEntity formSqlDatabase(com.azure.resourcemanager.sql.models.SqlDatabase databaseInner) {
        return SqlDatabaseEntity.builder().id(databaseInner.id())
            .name(databaseInner.name())
            .subscriptionId(ResourceId.fromString(databaseInner.id()).subscriptionId())
            .collation(databaseInner.collation())
            .creationDate(databaseInner.creationDate())
            .build();
    }

    private void refreshInnerIfNotSet() {
        if (Objects.isNull(this.sqlServerInner)) {
            this.refreshInner();
        }
    }

    synchronized void refreshInner() {
        try {
            sqlServerInner = StringUtils.isNotEmpty(entity.getId()) ?
                manager.sqlServers().getById(entity.getId()) :
                manager.sqlServers().getByResourceGroup(entity.getResourceGroup(), entity.getName());
            entity = this.fromSqlServer(sqlServerInner);
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            sqlServerInner = null;
        }
    }

    class SqlServerCreator extends ISqlServerCreator.AbstractSqlServerCreator<SqlServer> {

        @Override
        @AzureOperation(name = "sqlserver|server.create", params = {"this.getName()"}, type = AzureOperation.Type.SERVICE)
        public SqlServer commit() {
            // create
            sqlServerInner = SqlServer.this.manager.sqlServers().define(getName())
                .withRegion(getRegion().getName())
                .withExistingResourceGroup(getResourceGroupName())
                .withAdministratorLogin(getAdministratorLogin())
                .withAdministratorPassword(getAdministratorLoginPassword())
                .create();
            // update firewall rules
            SqlServer.this.update()
                .withEnableAccessFromAzureServices(SqlServer.this.entity.isEnableAccessFromAzureServices())
                .withEnableAccessFromLocalMachine(SqlServer.this.entity.isEnableAccessFromLocalMachine())
                .commit();
            // refresh entity
            SqlServer.this.refreshInner();
            return SqlServer.this;
        }
    }

    class SqlServerUpdater extends ISqlServerUpdater.AbstractSqlServerUpdater<SqlServer> {

        @Override
        public SqlServer commit() {
            SqlServer.this.refreshInnerIfNotSet();
            // update
            if (isEnableAccessFromAzureServices()) {
                sqlServerInner.enableAccessFromAzureServices();
            } else {
                sqlServerInner.removeAccessFromAzureServices();
            }
            // update common rule
            if (isEnableAccessFromLocalMachine()) {
                final String publicIp = getPublicIp(sqlServerInner);
                FirewallRuleEntity ruleEntity = FirewallRuleEntity.builder()
                    .name(FirewallRuleEntity.getAccessFromLocalFirewallRuleName()).startIpAddress(publicIp).endIpAddress(publicIp).build();
                new SqlFirewallRule(ruleEntity, sqlServerInner).create().commit();
            } else {
                FirewallRuleEntity ruleEntity = FirewallRuleEntity.builder().name(FirewallRuleEntity.getAccessFromLocalFirewallRuleName()).build();
                new SqlFirewallRule(ruleEntity, sqlServerInner).delete();
            }
            // refresh entity
            SqlServer.this.refreshInner();
            return SqlServer.this;
        }

        private String getPublicIp(final com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
            String ip;
            // try to get public IP by ping SQL Server
            String username = sqlServerInner.administratorLogin() + "@" + sqlServerInner.name();
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                DriverManager.getConnection(JdbcUrl.sqlserver(sqlServerInner.fullyQualifiedDomainName()).toString(), username, null);
            } catch (SQLException e) {
                ip = StringUtils.trim(NetUtils.parseIpAddressFromMessage(e.getMessage()));
                if (StringUtils.isNotBlank(ip) && NetUtils.INTACT_IPADDRESS_PATTERN.matcher(ip).find()) {
                    return ip;
                }
            } catch (ClassNotFoundException ignored) {
            }
            // Alternatively, get public IP by ping public URL
            ip = NetUtils.getPublicIp();
            if (StringUtils.isBlank(ip) || !NetUtils.INTACT_IPADDRESS_PATTERN.matcher(ip).find()) {
                throw new AzureToolkitRuntimeException("Failed to retrieve public IP in your environment, please confirm your network is available.");
            }
            return ip;
        }
    }

}
