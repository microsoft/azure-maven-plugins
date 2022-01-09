/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.Sku;
import com.azure.resourcemanager.mysql.models.SslEnforcementEnum;
import com.azure.resourcemanager.mysql.models.StorageProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MySqlServer extends AbstractAzResource<MySqlServer, MySqlResourceManager, Server>
    implements Removable, Startable {

    private final MySqlDatabaseModule databaseModule;
    private final MySqlFirewallRuleModule firewallRuleModule;

    protected MySqlServer(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull MySqlServerModule module) {
        super(name, resourceGroup, module);
        this.databaseModule = new MySqlDatabaseModule(this);
        this.firewallRuleModule = new MySqlFirewallRuleModule(this);
    }

    protected MySqlServer(@Nonnull String name, @Nonnull MySqlServerModule module) {
        this(name, module.getParent().getResourceGroupName(), module);
    }

    protected MySqlServer(@Nonnull Server remote, @Nonnull MySqlServerModule module) {
        this(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    @Override
    protected void refreshRemote() {
        this.remoteOptional().ifPresent(Server::refresh);
    }

    @Override
    public List<AzResourceModule<?, MySqlServer, ?>> getSubModules() {
        return Collections.emptyList();
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

    @AzureOperation(name = "mysql.start_server.server", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().start(this.getResourceGroupName(), this.getName()), Status.STARTING);
    }

    @AzureOperation(name = "mysql.stop_server.server", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().stop(this.getResourceGroupName(), this.getName()), Status.STOPPING);
    }

    @AzureOperation(name = "mysql.restart_server.server", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getParent().getRemote()).servers().restart(this.getResourceGroupName(), this.getName()), Status.RESTARTING);
    }

    public MySqlFirewallRuleModule firewallRules() {
        return this.firewallRuleModule;
    }

    public MySqlDatabaseModule databases() {
        return this.databaseModule;
    }

    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    public String getAdminName() {
        return remoteOptional().map(Server::administratorLogin).orElse(null);
    }

    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(Server::fullyQualifiedDomainName).orElse(null);
    }

    public boolean isAzureServiceAccessAllowed() {
        final String ruleName = MySqlFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    public boolean isLocalMachineAccessAllowed() {
        final String ruleName = MySqlFirewallRule.getLocalMachineAccessRuleName();
        return this.firewallRules().exists(ruleName, this.getResourceGroupName());
    }

    public String getVersion() {
        return remoteOptional().map(Server::version).map(ExpandableStringEnum::toString).orElse(null);
    }

    public String getState() {
        return remoteOptional().map(Server::userVisibleState).map(ExpandableStringEnum::toString).orElse(null);
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

    @Override
    public void remove() {
        this.delete();
    }
}
