/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql.service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.Database;
import com.azure.resourcemanager.mysql.models.Server;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/***
 * See: https://docs.microsoft.com/en-us/cli/azure/mysql/server?view=azure-cli-latest
 */
public class MySqlServer extends AbstractAzureResource<MySqlServer, MySqlEntity, Server> implements AzureOperationEvent.Source<MySqlServer>,
        IAzureResource<MySqlEntity> {
    @Nonnull
    private final MySqlManager manager;

    public MySqlServer(@Nonnull MySqlManager manager, @Nonnull Server server) {
        super(new MySqlEntity(server));
        this.manager = manager;
    }

    @Override
    protected Server loadRemote() {
        try {
            this.entity().setRemote(manager.servers().getById(this.entity.getId()));
        } catch (ManagementException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getResponse().getStatusCode()) {
                this.entity().setRemote(null);
            } else {
                throw ex;
            }
        }
        return this.entity.getRemote();
    }

    public String getPublicIpForLocalMachine() {
        // try to get public IP by ping MYSQL Server
        String username = entity.getAdministratorLoginName() + "@" + entity.getName();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            DriverManager.getConnection(JdbcUrl.mysql(entity.getFullyQualifiedDomainName()).toString(), username, null);
        } catch (SQLException e) {
            String ip = NetUtils.parseIpAddressFromMessage(e.getMessage());
            if (StringUtils.isNotBlank(ip)) {
                return ip;
            }
        } catch (ClassNotFoundException e) {
        }
        // Alternatively, get public IP by ping public URL
        return NetUtils.getPublicIp();
    }

    public MySqlFirewallRules firewallRules() {
        return new MySqlFirewallRules(this.manager, this.entity);
    }

    @AzureOperation(name = "mysql|server.delete", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        MySqlServer.this.manager.servers().deleteById(entity.getId());
    }

    @AzureOperation(name = "mysql|server.start", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        Preconditions.checkArgument(
            StringUtils.equalsIgnoreCase("Stopped", entity().getState()) ||
                StringUtils.equalsIgnoreCase("Disabled", entity().getState()),
            "Start action is not supported for non-disabled server.");
        MySqlServer.this.manager.servers().start(this.entity.getResourceGroup(), this.entity.getName());
    }

    @AzureOperation(name = "mysql|server.stop", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        Preconditions.checkArgument(StringUtils.equalsIgnoreCase("Ready", entity().getState()), "Stop action is not supported for non-ready server.");
        MySqlServer.this.manager.servers().stop(this.entity.getResourceGroup(), this.entity.getName());
    }

    @AzureOperation(name = "mysql|server.restart", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        Preconditions.checkArgument(StringUtils.equalsIgnoreCase("Ready", entity().getState()), "Restart action is not supported for non-ready server.");
        MySqlServer.this.manager.servers().restart(this.entity.getResourceGroup(), this.entity.getName());
    }

    public List<MySqlDatabaseEntity> databases() {
        return manager.databases().listByServer(this.entity.getResourceGroup(), this.entity.getName())
            .stream().map(this::toMySqlDatabaseEntity).collect(Collectors.toList());
    }

    private MySqlDatabaseEntity toMySqlDatabaseEntity(Database database) {
        return MySqlDatabaseEntity.builder().id(database.id()).name(database.name()).build();
    }
}
