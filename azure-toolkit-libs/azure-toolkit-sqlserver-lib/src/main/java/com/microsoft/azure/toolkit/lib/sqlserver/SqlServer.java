/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.SqlServerManager;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.IDatabaseServerUpdater;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SqlServer extends AbstractAzureResource<SqlServer, SqlServerEntity, com.azure.resourcemanager.sql.models.SqlServer>
        implements AzureOperationEvent.Source<SqlServer>, IAzureResource<SqlServerEntity>, IDatabaseServer {

    private final SqlServerManager manager;

    public SqlServer(@Nonnull SqlServerManager manager, @Nonnull com.azure.resourcemanager.sql.models.SqlServer server) {
        super(new SqlServerEntity(server));
        this.manager = manager;
    }

    @Override
    protected com.azure.resourcemanager.sql.models.SqlServer loadRemote() {
        try {
            return manager.sqlServers().getById(this.entity.getId());
        } catch (ManagementException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getResponse().getStatusCode()) {
                return null;
            }
            throw ex;
        }
    }

    @AzureOperation(name = "sqlserver|server.delete", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (StringUtils.isNotBlank(entity.getId())) {
            SqlServer.this.manager.sqlServers().deleteById(entity.getId());
        } else if (StringUtils.isNotBlank(entity.getResourceGroupName()) && StringUtils.isNotBlank(entity.getName())) {
            SqlServer.this.manager.sqlServers().deleteByResourceGroup(entity.getResourceGroupName(), entity.getName());
        } else {
            throw new AzureToolkitRuntimeException("Missing necessary parameters to delete SQL Server.");
        }
    }

    public IDatabaseServerUpdater<? extends SqlServer> update() {
        return new Updater();
    }

    public List<FirewallRuleEntity> firewallRules() {
        return this.remote().firewallRules().list().stream().map(this::formSqlServerFirewallRule).collect(Collectors.toList());
    }

    public List<SqlDatabaseEntity> databases() {
        return this.remote().databases().list().stream().map(this::formSqlDatabase).collect(Collectors.toList());
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

    class Updater extends IDatabaseServerUpdater.AbstractSqlServerUpdater<SqlServer> {

        @Override
        public SqlServer commit() {
            // update
            if (isEnableAccessFromAzureServices()) {
                SqlServer.this.remote().enableAccessFromAzureServices();
            } else {
                SqlServer.this.remote().removeAccessFromAzureServices();
            }
            // update common rule
            if (isEnableAccessFromLocalMachine()) {
                final String publicIp = getPublicIp(SqlServer.this.remote());
                FirewallRuleEntity ruleEntity = FirewallRuleEntity.builder()
                    .name(FirewallRuleEntity.getAccessFromLocalFirewallRuleName()).startIpAddress(publicIp).endIpAddress(publicIp).build();
                new SqlFirewallRule(ruleEntity, SqlServer.this.remote()).create().commit();
            } else {
                FirewallRuleEntity ruleEntity = FirewallRuleEntity.builder().name(FirewallRuleEntity.getAccessFromLocalFirewallRuleName()).build();
                new SqlFirewallRule(ruleEntity, SqlServer.this.remote()).delete();
            }
            // refresh entity
            SqlServer.this.loadRemote();
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
