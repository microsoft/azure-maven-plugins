/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class MicrosoftSqlFirewallRule extends AbstractAzResource<MicrosoftSqlFirewallRule, MicrosoftSqlServer, SqlFirewallRule> implements IFirewallRule {

    protected MicrosoftSqlFirewallRule(@Nonnull String name, @Nonnull MicrosoftSqlFirewallRuleModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    protected MicrosoftSqlFirewallRule(@Nonnull MicrosoftSqlFirewallRule origin) {
        super(origin);
    }

    protected MicrosoftSqlFirewallRule(@Nonnull SqlFirewallRule remote, @Nonnull MicrosoftSqlFirewallRuleModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull SqlFirewallRule remote) {
        return Status.OK;
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
