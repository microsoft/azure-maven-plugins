/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.core.util.logging.ClientLogger;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerCreator;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerFirewallUpdater;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlServer implements ISqlServer {
    private static final ClientLogger LOGGER = new ClientLogger(SqlServer.class);
    private static final String UNSUPPORTED_OPERATING_SYSTEM = "Unsupported operating system %s";
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

    @Override
    public SqlServerEntity entity() {
        return entity;
    }

    @Override
    public void delete() {
        this.manager.sqlServers().deleteById(this.sqlServerInner.id());
    }

    @Override
    public ISqlServerCreator<? extends ISqlServer> create() {
        return new SqlServerCreator()
            .withName(entity.getName())
            .withResourceGroup(entity.getResourceGroup())
            .withRegion(entity.getRegion())
            .withAdministratorLogin(entity.getAdministratorLoginName())
            .withEnableAccessFromAzureServices(entity.isEnableAccessFromAzureServices())
            .withEnableAccessFromLocalMachine(entity.isEnableAccessFromLocalMachine());
    }

    @Override
    public ISqlServerFirewallUpdater<? extends ISqlServer> update() {
        return new SqlServerFirewallUpdater()
            .withEnableAccessFromAzureServices(entity.isEnableAccessFromAzureServices())
            .withEnableAccessFromLocalMachine(entity.isEnableAccessFromLocalMachine());
    }

    private com.azure.resourcemanager.sql.models.SqlServer getSqlServerInner() {
        if (sqlServerInner == null) {
            refreshWebAppInner();
        }
        return sqlServerInner;
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

    synchronized void refreshWebAppInner() {
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
        public SqlServer commit() {
            // todo: Add validation for required parameters
            // create
            final com.azure.resourcemanager.sql.models.SqlServer server = SqlServer.this.manager.sqlServers().define(getName())
                .withRegion(getRegion().getName())
                .withExistingResourceGroup(getResourceGroupName())
                .withAdministratorLogin(getAdministratorLogin())
                .withAdministratorPassword(getAdministratorLoginPassword())
                .create();
            // update inner property
            SqlServer.this.sqlServerInner = server;
            // update entity properties after created sql server successfully.
            SqlServer.this.entity = SqlServer.this.fromSqlServer(server);
            // update
            if (isEnableAccessFromAzureServices() || isEnableAccessFromLocalMachine()) {
                new SqlServerFirewallUpdater().withEnableAccessFromAzureServices(isEnableAccessFromAzureServices())
                    .withEnableAccessFromLocalMachine(isEnableAccessFromLocalMachine()).commit();
            }
            return SqlServer.this;
        }
    }

    class SqlServerFirewallUpdater extends ISqlServerFirewallUpdater.AbstractSqlServerFirewallUpdater<SqlServer> {

        @Override
        public SqlServer commit() {
            List<SqlFirewallRule> firewallRuleList = sqlServerInner.firewallRules().list();
            if (isEnableAccessFromAzureServices()) {
                sqlServerInner.enableAccessFromAzureServices();
            } else if (serverEnableAccessFromAzureServices(firewallRuleList)) {
                sqlServerInner.removeAccessFromAzureServices();
            }
            if (isEnableAccessFromLocalMachine()) {
                enableAccessFromLocalMachine();
            } else if (serverEnableAccessFromLocalMachine(firewallRuleList)) {
                removeAccessFromLocalMachine();
            }
            refreshWebAppInner();
            // update entity properties after updated sql server successfully.
            SqlServer.this.entity = SqlServer.this.fromSqlServer(null);
            return null;
        }

        private boolean serverEnableAccessFromAzureServices(List<SqlFirewallRule> firewallRuleList) {
            return false;
            //sqlServerInner.firewallRules().list().stream().findAny(f -> StringUtils.equals(f));
        }

        private boolean serverEnableAccessFromLocalMachine(List<SqlFirewallRule> firewallRuleList) {
            return false;
        }

        private void enableAccessFromLocalMachine() {
            sqlServerInner.firewallRules().define("").withIpAddress("").create();
        }

        private void removeAccessFromLocalMachine() {
            sqlServerInner.firewallRules().delete("");
        }
    }

}
