/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class MicrosoftSqlFirewallRule extends AbstractAzResource<MicrosoftSqlFirewallRule, MicrosoftSqlServer, SqlFirewallRule> implements IFirewallRule {
    protected MicrosoftSqlFirewallRule(@Nonnull SqlFirewallRule rule, @Nonnull MicrosoftSqlFirewallRuleModule module) {
        this(rule.name(), module.getParent().getResourceGroupName(), module);
    }

    protected MicrosoftSqlFirewallRule(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MicrosoftSqlFirewallRuleModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public List<AzResourceModule<?, MicrosoftSqlFirewallRule, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SqlFirewallRule remote) {
        return Status.UNKNOWN;
    }

    @Override
    @Nullable
    public String getStartIpAddress() {
        return this.remoteOptional().map(SqlFirewallRule::startIpAddress).orElse(null);
    }

    @Override
    @Nullable
    public String getEndIpAddress() {
        return this.remoteOptional().map(SqlFirewallRule::endIpAddress).orElse(null);
    }
}
