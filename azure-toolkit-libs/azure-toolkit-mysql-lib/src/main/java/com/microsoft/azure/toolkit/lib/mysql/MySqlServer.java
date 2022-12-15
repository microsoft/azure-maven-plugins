/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysqlflexibleserver.models.Server;
import com.azure.resourcemanager.mysqlflexibleserver.models.ServerRestartParameter;
import com.azure.resourcemanager.mysqlflexibleserver.models.Sku;
import com.azure.resourcemanager.mysqlflexibleserver.models.Storage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MySqlServer extends AbstractAzResource<MySqlServer, MySqlServiceSubscription, Server>
    implements Deletable, Startable, IDatabaseServer<MySqlDatabase> {

    private final MySqlDatabaseModule databaseModule;
    private final MySqlFirewallRuleModule firewallRuleModule;

    protected MySqlServer(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull MySqlServerModule module) {
        super(name, resourceGroup, module);
        this.databaseModule = new MySqlDatabaseModule(this);
        this.firewallRuleModule = new MySqlFirewallRuleModule(this);
    }

    /**
     * copy constructor
     */
    protected MySqlServer(@Nonnull MySqlServer origin) {
        super(origin);
        this.databaseModule = origin.databaseModule;
        this.firewallRuleModule = origin.firewallRuleModule;
    }

    protected MySqlServer(@Nonnull Server remote, @Nonnull MySqlServerModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.databaseModule = new MySqlDatabaseModule(this);
        this.firewallRuleModule = new MySqlFirewallRuleModule(this);
    }

    @Nullable
    @Override
    protected Server refreshRemoteFromAzure(@Nonnull Server remote) {
        return remote.refresh();
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(this.firewallRuleModule, this.databaseModule);
    }

    @Nonnull
    public MySqlFirewallRuleModule firewallRules() {
        return this.firewallRuleModule;
    }

    @Nonnull
    public MySqlDatabaseModule databases() {
        return this.databaseModule;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull Server remote) {
        return remote.state().toString();
    }

    @AzureOperation(name = "resource.start_resource_in_azure.resource", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().start(this.getResourceGroupName(), this.getName()), Status.STARTING);
    }

    @AzureOperation(name = "resource.stop_resource_in_azure.resource", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().stop(this.getResourceGroupName(), this.getName()), Status.STOPPING);
    }

    @AzureOperation(name = "resource.restart_resource_in_azure.resource", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public void restart() {
        final ServerRestartParameter parameter = new ServerRestartParameter();
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().restart(this.getResourceGroupName(), this.getName(), parameter), Status.RESTARTING);
    }

    @Nullable
    @Override
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    @Override
    public String getAdminName() {
        return remoteOptional().map(Server::administratorLogin).orElse(null);
    }

    @Nullable
    @Override
    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(Server::fullyQualifiedDomainName).orElse(null);
    }

    @Override
    public boolean isAzureServiceAccessAllowed() {
        final String ruleName = IFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    @Override
    public boolean isLocalMachineAccessAllowed() {
        final String ruleName = IFirewallRule.getLocalMachineAccessRuleName();
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    @Nullable
    @Override
    public String getVersion() {
        return remoteOptional().map(Server::version).map(ExpandableStringEnum::toString).orElse(null);
    }

    @Nullable
    @Override
    public String getType() {
        return remoteOptional().map(Server::type).orElse(null);
    }

    @Nullable
    public String getSkuTier() {
        return remoteOptional().map(Server::sku).map(Sku::tier).map(ExpandableStringEnum::toString).orElse(null);
    }

    public int getStorageInMB() {
        return remoteOptional().map(Server::storage).map(Storage::storageSizeGB).map(s -> s * 1024).orElse(0);
    }

    @Nonnull
    @Override
    public String getLocalMachinePublicIp() {
        // try to get public IP by ping MYSQL Server
        String username = this.getAdminName() + "@" + this.getName();
        try {
            Class.forName("com.mysql.jdbc.Driver");
            DriverManager.getConnection(JdbcUrl.mysql(this.getFullyQualifiedDomainName()).toString(), username, null);
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

    @Nonnull
    public JdbcUrl getJdbcUrl() {
        return JdbcUrl.mysql(this.getFullyQualifiedDomainName());
    }

    @Nonnull
    @Override
    public List<MySqlDatabase> listDatabases() {
        return this.databases().list();
    }

    @Override
    public boolean isStoppable() {
        return StringUtils.equalsIgnoreCase(this.getStatus(), "Ready");
    }

    @Override
    public boolean isStartable() {
        return StringUtils.equalsIgnoreCase(this.getStatus(), "Stopped");
    }
}
