/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
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

public class MicrosoftSqlServer extends AbstractAzResource<MicrosoftSqlServer, MicrosoftSqlServiceSubscription, SqlServer>
    implements Deletable, IDatabaseServer<MicrosoftSqlDatabase> {

    @Nonnull
    private final MicrosoftSqlDatabaseModule databaseModule;
    @Nonnull
    private final MicrosoftSqlFirewallRuleModule firewallRuleModule;

    protected MicrosoftSqlServer(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MicrosoftSqlServerModule module) {
        super(name, resourceGroupName, module);
        this.databaseModule = new MicrosoftSqlDatabaseModule(this);
        this.firewallRuleModule = new MicrosoftSqlFirewallRuleModule(this);
    }

    /**
     * copy constructor
     */
    public MicrosoftSqlServer(@Nonnull MicrosoftSqlServer origin) {
        super(origin);
        this.databaseModule = origin.databaseModule;
        this.firewallRuleModule = origin.firewallRuleModule;
    }

    protected MicrosoftSqlServer(@Nonnull SqlServer remote, @Nonnull MicrosoftSqlServerModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.databaseModule = new MicrosoftSqlDatabaseModule(this);
        this.firewallRuleModule = new MicrosoftSqlFirewallRuleModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, MicrosoftSqlServer, ?>> getSubModules() {
        return Arrays.asList(this.firewallRuleModule, this.databaseModule);
    }

    @Nonnull
    public MicrosoftSqlFirewallRuleModule firewallRules() {
        return this.firewallRuleModule;
    }

    @Nonnull
    public MicrosoftSqlDatabaseModule databases() {
        return this.databaseModule;
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SqlServer remote) {
        return remote.state();
    }

    @Nullable
    @Override
    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    @Nullable
    @Override
    public String getAdminName() {
        return remoteOptional().map(SqlServer::administratorLogin).orElse(null);
    }

    @Nullable
    @Override
    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(SqlServer::fullyQualifiedDomainName).orElse(null);
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
        return remoteOptional().map(SqlServer::version).orElse(null);
    }

    @Nullable
    @Override
    public String getType() {
        return remoteOptional().map(SqlServer::type).orElse(null);
    }

    @Nonnull
    @Override
    public String getLocalMachinePublicIp() {
        String ip;
        // try to get public IP by ping SQL SqlServer
        String username = this.getAdminName() + "@" + this.getName();
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            DriverManager.getConnection(JdbcUrl.sqlserver(this.getFullyQualifiedDomainName()).toString(), username, null);
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

    @Nonnull
    public JdbcUrl getJdbcUrl() {
        return JdbcUrl.sqlserver(this.getFullyQualifiedDomainName());
    }

    @Nonnull
    @Override
    public List<MicrosoftSqlDatabase> listDatabases() {
        return this.databases().list();
    }
}
