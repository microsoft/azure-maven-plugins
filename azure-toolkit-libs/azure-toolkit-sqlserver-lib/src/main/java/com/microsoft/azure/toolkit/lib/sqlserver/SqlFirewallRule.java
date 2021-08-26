/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleConfig;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;

public class SqlFirewallRule {

    private FirewallRuleEntity entity;
    private final com.azure.resourcemanager.sql.models.SqlServer sqlServerInner;

    public SqlFirewallRule(FirewallRuleEntity entity, com.azure.resourcemanager.sql.models.SqlServer sqlServerInner) {
        this.entity = entity;
        this.sqlServerInner = sqlServerInner;
    }

    public Creator create() {
        FirewallRuleConfig config = FirewallRuleConfig.builder()
                .name(entity.getName()).startIpAddress(entity.getStartIpAddress()).endIpAddress(entity.getEndIpAddress()).build();
        return new Creator(config);
    }

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

    class Creator implements ICommittable<SqlFirewallRule> {

        private FirewallRuleConfig config;

        Creator(FirewallRuleConfig config) {
            this.config = config;
        }

        @Override
        public SqlFirewallRule commit() {
            com.azure.resourcemanager.sql.models.SqlFirewallRule rule = sqlServerInner.firewallRules()
                    .define(config.getName())
                    .withIpAddressRange(config.getStartIpAddress(), config.getEndIpAddress())
                    .create();
            SqlFirewallRule.this.entity = fromSqlFirewallRule(rule);
            return SqlFirewallRule.this;
        }
    }

}
