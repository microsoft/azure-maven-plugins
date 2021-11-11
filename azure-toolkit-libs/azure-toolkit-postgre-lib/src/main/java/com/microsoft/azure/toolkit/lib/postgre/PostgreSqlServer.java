/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Database;
import com.azure.resourcemanager.postgresql.models.Server;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.annotation.Nonnull;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/***
 * See: https://docs.microsoft.com/en-us/cli/azure/postgres/server?view=azure-cli-latest
 */
@Slf4j
public class PostgreSqlServer extends AbstractAzureResource<PostgreSqlServer, PostgreSqlServerEntity, Server> implements AzureOperationEvent.Source<PostgreSqlServer>,
        IAzureResource<PostgreSqlServerEntity>, IDatabaseServer, Startable<PostgreSqlServerEntity>, Removable {
    public static final String NOT_SUPPORTED_BY_AZURE_POSTGRE_SQL = "Start and stop are not supported by Azure PostgreSQL.";
    @Nonnull
    private final PostgreSqlManager manager;

    public PostgreSqlServer(@Nonnull PostgreSqlManager manager, @Nonnull Server server) {
        super(new PostgreSqlServerEntity(manager, server));
        this.manager = manager;
    }

    @Override
    protected Server loadRemote() {
        try {
            return manager.servers().getById(this.entity.getId());
        } catch (ManagementException ex) {
            if (HttpStatus.SC_NOT_FOUND == ex.getResponse().getStatusCode()) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public PostgreSqlServer refresh() {
        try {
            return super.refresh();
        } finally {
            try {
                CacheManager.evictCache("postgre/{}", this.id());
                CacheManager.evictCache("postgre/{}/rg/{}/postgre/{}", String.format("%s/%s/%s", this.subscriptionId(), this.resourceGroup(), this.name()));
            } catch (Throwable e) {
                log.warn("failed to evict cache", e);
            }
        }
    }

    public String getPublicIpForLocalMachine() {
        // try to get public IP by ping PostgreSQL Server
        String username = entity.getAdministratorLoginName() + "@" + entity.getName();
        try {
            Class.forName("org.postgresql.Driver");
            DriverManager.getConnection(JdbcUrl.postgre(entity.getFullyQualifiedDomainName()).toString(), username, null);
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

    public PostgreSqlFirewallRules firewallRules() {
        return new PostgreSqlFirewallRules(this.manager, this.entity);
    }

    @AzureOperation(name = "postgre|server.delete", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void delete() {
        if (this.exists()) {
            this.status(Status.PENDING);
            PostgreSqlServer.this.manager.servers().deleteById(entity.getId());
            Azure.az(AzurePostgreSql.class).refresh();
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_BY_AZURE_POSTGRE_SQL);
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_BY_AZURE_POSTGRE_SQL);
    }

    @AzureOperation(name = "postgre|server.restart", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        Preconditions.checkArgument(StringUtils.equalsIgnoreCase("Ready", entity().getState()), "Restart action is not supported for non-ready server.");
        if (this.exists()) {
            this.status(Status.PENDING);
            PostgreSqlServer.this.manager.servers().restart(this.entity.getResourceGroupName(), this.entity.getName());
            this.refresh();
        }
    }

    public List<PostgreSqlDatabaseEntity> databases() {
        return manager.databases().listByServer(this.entity.getResourceGroupName(), this.entity.getName())
            .stream().map(this::toPostgreSqlDatabaseEntity).collect(Collectors.toList());
    }

    public List<PostgreSqlDatabase> databasesV2() {
        return manager.databases().listByServer(this.entity.getResourceGroupName(), this.entity.getName())
                .stream().map(this::toPostgreSqlDatabase).collect(Collectors.toList());
    }

    public PostgreSqlDatabase database(@Nonnull String databaseName) {
        return toPostgreSqlDatabase(manager.databases().get(this.entity.getResourceGroupName(), this.entity.getName(), databaseName));
    }

    private PostgreSqlDatabase toPostgreSqlDatabase(Database database) {
        return new PostgreSqlDatabase(manager, toPostgreSqlDatabaseEntity(database));
    }

    private PostgreSqlDatabaseEntity toPostgreSqlDatabaseEntity(Database database) {
        return new PostgreSqlDatabaseEntity(manager, database);
    }

    @Override
    public void remove() {
        delete();
    }
}
