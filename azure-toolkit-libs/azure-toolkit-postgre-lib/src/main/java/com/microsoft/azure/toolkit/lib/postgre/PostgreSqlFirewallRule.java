/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.FirewallRule;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PostgreSqlFirewallRule {
    private final PostgreSqlManager manager;
    private final PostgreSqlServerEntity serverEntity;
    @Getter
    private FirewallRuleEntity entity;

    static FirewallRuleEntity fromFirewallRule(FirewallRule firewallRule) {
        return FirewallRuleEntity.builder()
            .id(firewallRule.id())
            .name(firewallRule.name())
            .startIpAddress(firewallRule.startIpAddress())
            .endIpAddress(firewallRule.endIpAddress()).build();
    }

    public void delete() {
        manager.firewallRules().delete(serverEntity.getResourceGroupName(), this.serverEntity.getName(), this.entity.getName());
    }

    public ICommittable<PostgreSqlFirewallRule> update(String startIpAddress, String endIpAddress) {
        return () -> {
            FirewallRule rule = manager.firewallRules().get(serverEntity.getResourceGroupName(),
                PostgreSqlFirewallRule.this.serverEntity.getName(), PostgreSqlFirewallRule.this.entity.getName()).update()
                .withStartIpAddress(startIpAddress)
                .withEndIpAddress(endIpAddress).apply();
            PostgreSqlFirewallRule.this.entity = fromFirewallRule(rule);
            return PostgreSqlFirewallRule.this;
        };
    }
}
