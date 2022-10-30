/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.FirewallRule;
import com.azure.resourcemanager.postgresql.models.FirewallRules;
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

public class PostgreSqlFirewallRuleModule extends AbstractAzResourceModule<PostgreSqlFirewallRule, PostgreSqlServer, FirewallRule> {
    public static final String NAME = "firewallRules";

    public PostgreSqlFirewallRuleModule(@Nonnull PostgreSqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected PostgreSqlFirewallRule newResource(@Nonnull FirewallRule rule) {
        return new PostgreSqlFirewallRule(rule, this);
    }

    @Nonnull
    @Override
    protected PostgreSqlFirewallRule newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new PostgreSqlFirewallRule(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<FirewallRule> loadResourcesFromAzure() {
        final PostgreSqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.listByServer(p.getResourceGroupName(), p.getName()).stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected FirewallRule loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        final PostgreSqlServer p = this.getParent();
        return Optional.ofNullable(this.getClient()).map(c -> c.get(p.getResourceGroupName(), p.getName(), name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "postgre.delete_firewall_rule_in_azure.rule", params = {"nameFromResourceId(id)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final PostgreSqlServer p = this.getParent();
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(p.getResourceGroupName(), p.getName(), name));
    }

    @Nonnull
    @Override
    protected PostgreSqlFirewallRuleDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new PostgreSqlFirewallRuleDraft(name, this);
    }

    @Nonnull
    @Override
    protected PostgreSqlFirewallRuleDraft newDraftForUpdate(@Nonnull PostgreSqlFirewallRule origin) {
        return new PostgreSqlFirewallRuleDraft(origin);
    }

    @Nullable
    @Override
    protected FirewallRules getClient() {
        return Optional.ofNullable(this.getParent().getParent().getRemote()).map(PostgreSqlManager::firewallRules).orElse(null);
    }

    void toggleAzureServiceAccess(boolean allowed) {
        final String ruleName = IFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        final String rgName = this.getParent().getResourceGroupName();
        final boolean exists = this.exists(ruleName, rgName);
        if (!allowed && exists) {
            this.delete(ruleName, rgName);
        }
        if (allowed && !exists) {
            final PostgreSqlFirewallRuleDraft draft = this.create(ruleName, rgName);
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
                "Cannot enable local machine access to PostgreSql server due to error: cannot get public ip.");
            final PostgreSqlFirewallRuleDraft draft = this.updateOrCreate(ruleName, rgName);
            draft.setStartIpAddress(publicIp);
            draft.setEndIpAddress(publicIp);
            draft.commit();
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "PostgreSQL firewall rule";
    }
}
