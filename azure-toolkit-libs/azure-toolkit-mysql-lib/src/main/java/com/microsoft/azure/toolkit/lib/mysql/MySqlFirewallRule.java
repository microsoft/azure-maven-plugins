/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.FirewallRule;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlServerEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class MySqlFirewallRule {
    private final MySqlManager manager;
    private final MySqlServerEntity serverEntity;
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

    public ICommittable<MySqlFirewallRule> update(String startIpAddress, String endIpAddress) {
        return () -> {
            FirewallRule rule = manager.firewallRules().get(serverEntity.getResourceGroupName(),
                MySqlFirewallRule.this.serverEntity.getName(), MySqlFirewallRule.this.entity.getName()).update()
                .withStartIpAddress(startIpAddress)
                .withEndIpAddress(endIpAddress).apply();
            MySqlFirewallRule.this.entity = fromFirewallRule(rule);
            return MySqlFirewallRule.this;
        };
    }
}
