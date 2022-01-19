/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.Sku;
import com.azure.resourcemanager.postgresql.models.SslEnforcementEnum;
import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PostgreSqlServer extends AbstractAzResource<PostgreSqlServer, PostgreSqlResourceManager, Server>
    implements Removable, Startable, IDatabaseServer<PostgreSqlDatabase> {

    private final PostgreSqlDatabaseModule databaseModule;
    private final PostgreSqlFirewallRuleModule firewallRuleModule;

    protected PostgreSqlServer(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull PostgreSqlServerModule module) {
        super(name, resourceGroupName, module);
        this.databaseModule = new PostgreSqlDatabaseModule(this);
        this.firewallRuleModule = new PostgreSqlFirewallRuleModule(this);
    }

    /**
     * copy constructor
     */
    public PostgreSqlServer(@Nonnull PostgreSqlServer origin) {
        super(origin);
        this.databaseModule = origin.databaseModule;
        this.firewallRuleModule = origin.firewallRuleModule;
    }

    protected PostgreSqlServer(@Nonnull Server remote, @Nonnull PostgreSqlServerModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.databaseModule = new PostgreSqlDatabaseModule(this);
        this.firewallRuleModule = new PostgreSqlFirewallRuleModule(this);
        this.setRemote(remote);
    }

    @Override
    protected Server refreshRemote() {
        return this.remoteOptional().map(Server::refresh).orElse(null);
    }

    @Override
    public List<AzResourceModule<?, PostgreSqlServer, ?>> getSubModules() {
        return Arrays.asList(this.firewallRuleModule, this.databaseModule);
    }

    public PostgreSqlFirewallRuleModule firewallRules() {
        return this.firewallRuleModule;
    }

    public PostgreSqlDatabaseModule databases() {
        return this.databaseModule;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Server remote) {
        return remote.userVisibleState().toString();
    }

    @Override
    public String status() {
        return this.getStatus();
    }

    @Override
    public void start() {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public void stop() {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @AzureOperation(name = "postgre.restart_server.server", params = {"this.entity().getName()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).restart(), Status.RESTARTING);
    }

    @Override
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Override
    public String getAdminName() {
        return remoteOptional().map(Server::administratorLogin).orElse(null);
    }

    @Override
    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(Server::fullyQualifiedDomainName).orElse(null);
    }

    @Override
    public boolean isAzureServiceAccessAllowed() {
        final String ruleName = PostgreSqlFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    @Override
    public boolean isLocalMachineAccessAllowed() {
        final String ruleName = IFirewallRule.getLocalMachineAccessRuleName();
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    @Override
    public String getVersion() {
        return remoteOptional().map(Server::version).map(ExpandableStringEnum::toString).orElse(null);
    }

    @Override
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

    @Override
    public String getLocalMachinePublicIp() {
        // try to get public IP by ping PostgreSQL Server
        String username = this.getAdminName() + "@" + this.getName();
        try {
            Class.forName("org.postgresql.Driver");
            DriverManager.getConnection(JdbcUrl.postgre(this.getFullyQualifiedDomainName(), "postgre").toString(), username, null);
        } catch (SQLException e) {
            String ip = NetUtils.parseIpAddressFromMessage(e.getMessage());
            if (StringUtils.isNotBlank(ip)) {
                return ip;
            }
        } catch (ClassNotFoundException ignored) {
        }
        // Alternatively, get public IP by ping public URL
        return NetUtils.getPublicIp();
    }

    public JdbcUrl getJdbcUrl() {
        return JdbcUrl.postgre(this.getFullyQualifiedDomainName(), "postgre");
    }

    @Override
    public List<PostgreSqlDatabase> listDatabases() {
        return this.databases().list();
    }

    @Override
    public void remove() {
        this.delete();
    }

    @Override
    public boolean isStoppable() {
        return StringUtils.equalsIgnoreCase(this.getStatus(), "Ready");
    }
}
