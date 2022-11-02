/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.FirewallRule;
import com.azure.resourcemanager.mysql.models.FirewallRules;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class MySqlFirewallRuleModule extends AbstractAzResourceModule<MySqlFirewallRule, MySqlServer, FirewallRule> {
    public static final String NAME = "firewallRules";

    public MySqlFirewallRuleModule(@Nonnull MySqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected MySqlFirewallRule newResource(@Nonnull FirewallRule rule) {
        return new MySqlFirewallRule(rule, this);
    }

    @Nonnull
    @Override
    protected MySqlFirewallRule newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MySqlFirewallRule(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<FirewallRule> loadResourcesFromAzure() {
        final MySqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.listByServer(p.getResourceGroupName(), p.getName()).stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource_in_azure.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected FirewallRule loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        final MySqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.get(p.getResourceGroupName(), p.getName(), name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "mysql.delete_firewall_rule_in_azure.rule", params = {"nameFromResourceId(id)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final MySqlServer p = this.getParent();
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(p.getResourceGroupName(), p.getName(), name));
    }

    @Nonnull
    @Override
    protected MySqlFirewallRuleDraft newDraftForCreate(@Nonnull String name, String resourceGroupName) {
        return new MySqlFirewallRuleDraft(name, this);
    }

    @Nonnull
    @Override
    protected MySqlFirewallRuleDraft newDraftForUpdate(@Nonnull MySqlFirewallRule origin) {
        return new MySqlFirewallRuleDraft(origin);
    }

    @Nullable
    @Override
    protected FirewallRules getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(MySqlManager::firewallRules).orElse(null);
    }

    void toggleAzureServiceAccess(boolean allowed) {
        final String ruleName = IFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        final String rgName = this.getParent().getResourceGroupName();
        final boolean exists = this.exists(ruleName, rgName);
        if (!allowed && exists) {
            this.delete(ruleName, rgName);
        }
        if (allowed && !exists) {
            final MySqlFirewallRuleDraft draft = this.create(ruleName, rgName);
            draft.setStartIpAddress(IFirewallRule.IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
            draft.setEndIpAddress(IFirewallRule.IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
            draft.commit();
        }
    }

    void toggleLocalMachineAccess(boolean allowed) {
        final String ruleName = IFirewallRule.getLocalMachineAccessRuleName();
        final String rgName = this.getParent().getResourceGroupName();
        final boolean exists = this.exists(ruleName, rgName);
        if (!allowed && exists) {
            this.delete(ruleName, rgName);
        }
        if (allowed && !exists) {
            final String publicIp = this.getParent().getLocalMachinePublicIp();
            Preconditions.checkArgument(StringUtils.isNotBlank(publicIp),
                "Cannot enable local machine access to MySQL server due to error: cannot get public ip.");
            final MySqlFirewallRuleDraft draft = this.updateOrCreate(ruleName, rgName);
            draft.setStartIpAddress(publicIp);
            draft.setEndIpAddress(publicIp);
            draft.commit();
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "MySQL firewall rule";
    }
}
