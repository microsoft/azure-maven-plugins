/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service.impl;

import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlFirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlFirewallRule;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlFirewallRuleCreator;

public class SqlFirewallRuleRule implements ISqlFirewallRule {

    private SqlFirewallRuleEntity entity;
    private com.azure.resourcemanager.sql.models.SqlServer sqlServerInner;

    public SqlFirewallRuleRule(SqlFirewallRuleEntity entity, com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        this.entity = entity;
        this.sqlServerInner = sqlServerInner;
    }

    @Override
    public ISqlFirewallRuleCreator<? extends ISqlFirewallRule> create() {
        return new SqlFirewallRuleCreator()
                .withName(entity.getName())
                .wihStartIpAddress(entity.getStartIpAddress())
                .withEndIpAddress(entity.getEndIpAddress());
    }

    @Override
    public void delete() {
        sqlServerInner.firewallRules().delete(entity.getName());
    }

    class SqlFirewallRuleCreator extends ISqlFirewallRuleCreator.AbstractSqlFirewallRuleCreator<SqlFirewallRuleRule> {

        @Override
        public SqlFirewallRuleRule commit() {
            com.azure.resourcemanager.sql.models.SqlFirewallRule rule = sqlServerInner.firewallRules().define(getName()).withIpAddressRange(getStartIpAddress(), getEndIpAddress()).create();
            return SqlFirewallRuleRule.this;
        }
    }

}
