/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.models.FirewallRule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.database.entity.IFirewallRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class PostgreSqlFirewallRule extends AbstractAzResource<PostgreSqlFirewallRule, PostgreSqlServer, FirewallRule> implements IFirewallRule {

    protected PostgreSqlFirewallRule(@Nonnull String name, @Nonnull PostgreSqlFirewallRuleModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    protected PostgreSqlFirewallRule(@Nonnull PostgreSqlFirewallRule origin) {
        super(origin);
    }

    protected PostgreSqlFirewallRule(@Nonnull FirewallRule remote, @Nonnull PostgreSqlFirewallRuleModule module) {
        super(remote.name(), module);
        this.setRemote(remote);
    }

    @Override
    protected FirewallRule refreshRemote() {
        return this.remoteOptional().map(FirewallRule::refresh).orElse(null);
    }

    @Override
    public List<AzResourceModule<?, PostgreSqlFirewallRule, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull FirewallRule remote) {
        return Status.UNKNOWN;
    }

    @Nullable
    public String getStartIpAddress() {
        return this.remoteOptional().map(FirewallRule::startIpAddress).orElse(null);
    }

    @Nullable
    public String getEndIpAddress() {
        return this.remoteOptional().map(FirewallRule::endIpAddress).orElse(null);
    }
}
