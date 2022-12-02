/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.azure.resourcemanager.sql.models.SqlFirewallRuleOperations;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class MicrosoftSqlFirewallRuleModule extends AbstractAzResourceModule<MicrosoftSqlFirewallRule, MicrosoftSqlServer, SqlFirewallRule> {
    public static final String NAME = "firewallRules";

    public MicrosoftSqlFirewallRuleModule(@Nonnull MicrosoftSqlServer parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlFirewallRule newResource(@Nonnull SqlFirewallRule rule) {
        return new MicrosoftSqlFirewallRule(rule, this);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlFirewallRule newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MicrosoftSqlFirewallRule(name, this);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected Stream<SqlFirewallRule> loadResourcesFromAzure() {
        return Optional.ofNullable(this.getClient()).map(c -> c.list().stream()).orElse(Stream.empty());
    }

    @Nullable
    @Override
    @AzureOperation(name = "azure/resource.load_resource.resource|type", params = {"name", "this.getResourceTypeName()"}, type = AzureOperation.Type.REQUEST)
    protected SqlFirewallRule loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        return Optional.ofNullable(this.getClient()).map(c -> c.get(name)).orElse(null);
    }

    @Override
    @AzureOperation(name = "azure/sqlserver.delete_firewall_rule.rule", params = {"nameFromResourceId(id)"}, type = AzureOperation.Type.REQUEST)
    protected void deleteResourceFromAzure(@Nonnull String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        final String name = resourceId.name();
        Optional.ofNullable(this.getClient()).ifPresent(c -> c.delete(name));
    }

    @Nonnull
    @Override
    protected MicrosoftSqlFirewallRuleDraft newDraftForCreate(@Nonnull String name, @Nullable String resourceGroupName) {
        assert resourceGroupName != null : "'Resource group' is required.";
        return new MicrosoftSqlFirewallRuleDraft(name, this);
    }

    @Nonnull
    @Override
    protected MicrosoftSqlFirewallRuleDraft newDraftForUpdate(@Nonnull MicrosoftSqlFirewallRule origin) {
        return new MicrosoftSqlFirewallRuleDraft(origin);
    }

    @Nullable
    @Override
    protected SqlFirewallRuleOperations.SqlFirewallRuleActionsDefinition getClient() {
        return Optional.ofNullable(this.getParent().getRemote()).map(SqlServer::firewallRules).orElse(null);
    }

    void toggleAzureServiceAccess(boolean allowed) {
        final String ruleName = IFirewallRule.AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME;
        final String rgName = this.getParent().getResourceGroupName();
        final boolean exists = this.exists(ruleName, rgName);
        if (!allowed && exists) {
            this.delete(ruleName, rgName);
        }
        if (allowed && !exists) {
            final MicrosoftSqlFirewallRuleDraft draft = this.create(ruleName, rgName);
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
                "Cannot enable local machine access to SqlServer due to error: cannot get public ip.");
            final MicrosoftSqlFirewallRuleDraft draft = this.updateOrCreate(ruleName, rgName);
            draft.setStartIpAddress(publicIp);
            draft.setEndIpAddress(publicIp);
            draft.commit();
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SQL server firewall rule";
    }
}
