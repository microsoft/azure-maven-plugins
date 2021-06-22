/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service.impl;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.database.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlFirewallRule;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlFirewallRuleCreator;

public class SqlFirewallRule implements ISqlFirewallRule {

    private FirewallRuleEntity entity;
    private com.azure.resourcemanager.sql.models.SqlServer sqlServerInner;

    public SqlFirewallRule(FirewallRuleEntity entity, com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
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

    private FirewallRuleEntity fromSqlFirewallRule(com.azure.resourcemanager.sql.models.SqlFirewallRule rule) {
        return FirewallRuleEntity.builder().name(rule.name())
                .id(rule.id())
                .subscriptionId(ResourceId.fromString(rule.id()).subscriptionId())
                .startIpAddress(rule.startIpAddress())
                .endIpAddress(rule.endIpAddress())
                .build();
    }

    class SqlFirewallRuleCreator extends ISqlFirewallRuleCreator.AbstractSqlFirewallRuleCreator<SqlFirewallRule> {

        @Override
        public SqlFirewallRule commit() {
            com.azure.resourcemanager.sql.models.SqlFirewallRule rule = sqlServerInner.firewallRules().define(getName()).withIpAddressRange(getStartIpAddress(), getEndIpAddress()).create();
            SqlFirewallRule.this.entity = fromSqlFirewallRule(rule);
            return SqlFirewallRule.this;
        }
    }

}
