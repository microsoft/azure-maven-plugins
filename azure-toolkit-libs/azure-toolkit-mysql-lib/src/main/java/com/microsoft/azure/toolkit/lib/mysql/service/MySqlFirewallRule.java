/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql.service;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.FirewallRule;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class MySqlFirewallRule {
    private final MySqlManager manager;
    private final MySqlEntity mySqlEntity;
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
        manager.firewallRules().delete(mySqlEntity.getResourceGroup(), this.mySqlEntity.getName(), this.entity.getName());
    }

    public ICommittable<MySqlFirewallRule> update(String startIpAddress, String endIpAddress) {
        return () -> {
            FirewallRule rule = manager.firewallRules().get(mySqlEntity.getResourceGroup(),
                MySqlFirewallRule.this.mySqlEntity.getName(), MySqlFirewallRule.this.entity.getName()).update()
                .withStartIpAddress(startIpAddress)
                .withEndIpAddress(endIpAddress).apply();
            MySqlFirewallRule.this.entity = fromFirewallRule(rule);
            return MySqlFirewallRule.this;
        };
    }
}
