/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service.impl;

import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerFirewallEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerFirewall;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServerFirewallCreator;

public class SqlServerFirewall implements ISqlServerFirewall {

    private SqlServerFirewallEntity entity;
    private com.azure.resourcemanager.sql.models.SqlServer sqlServerInner;

    public SqlServerFirewall(SqlServerFirewallEntity entity, com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        this.entity = entity;
        this.sqlServerInner = sqlServerInner;
    }

    @Override
    public ISqlServerFirewallCreator<? extends ISqlServerFirewall> create() {
        return new SqlServerFirewallCreator()
                .withName(entity.getName())
                .wihStartIpAddress(entity.getStartIpAddress())
                .withEndIpAddress(entity.getEndIpAddress());
    }

    class SqlServerFirewallCreator extends ISqlServerFirewallCreator.AbstractSqlServerFirewallCreator<SqlServerFirewall> {

        @Override
        public SqlServerFirewall commit() {
            SqlFirewallRule rule = sqlServerInner.firewallRules().define(getName()).withIpAddressRange(getStartIpAddress(), getEndIpAddress()).create();
            return SqlServerFirewall.this;
        }
    }

}
