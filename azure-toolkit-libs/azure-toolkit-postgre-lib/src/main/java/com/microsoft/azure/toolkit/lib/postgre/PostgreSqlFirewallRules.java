/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.FirewallRule;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleConfig;
import com.microsoft.azure.toolkit.lib.database.entity.FirewallRuleEntity;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerEntity;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class PostgreSqlFirewallRules {

    private final PostgreSqlManager manager;
    private final PostgreSqlServerEntity serverEntity;

    public List<PostgreSqlFirewallRule> list() {
        return manager.firewallRules().listByServer(serverEntity.getResourceGroupName(), this.serverEntity.getName()).stream()
            .map(this::fromFirewallRule).collect(Collectors.toList());
    }

    private PostgreSqlFirewallRule fromFirewallRule(FirewallRule firewallRule) {
        return new PostgreSqlFirewallRule(manager, serverEntity, PostgreSqlFirewallRule.fromFirewallRule(firewallRule));
    }

    public Creator create(FirewallRuleConfig config) {
        return new Creator(config);
    }

    public PostgreSqlFirewallRule enableLocalMachineAccessRule(String publicIp) {
        Preconditions.checkArgument(StringUtils.isNotBlank(publicIp),
            "Cannot enable local machine access to postgre sql server due to error: cannot get public ip.");
        String name = FirewallRuleEntity.getAccessFromLocalFirewallRuleName();
        PostgreSqlFirewallRule myFirewallRule = getAzureAccessRuleByName(name);
        if (myFirewallRule != null) {
            if (myFirewallRule.getEntity() != null && StringUtils.equals(myFirewallRule.getEntity().getStartIpAddress(), publicIp)) {
                return myFirewallRule;
            }
            return myFirewallRule.update(publicIp, publicIp).commit();
        } else {
            return create(FirewallRuleConfig.builder().name(name).startIpAddress(publicIp).endIpAddress(publicIp).build()).commit();
        }
    }

    public PostgreSqlFirewallRule enableAzureAccessRule() {
        PostgreSqlFirewallRule allowAzureAccessRule = getAzureAccessRule();
        if (allowAzureAccessRule != null) {
            return allowAzureAccessRule;
        }
        return create(FirewallRuleConfig.builder()
            .name(FirewallRuleEntity.ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME)
            .startIpAddress(FirewallRuleEntity.IP_ALLOW_ACCESS_TO_AZURE_SERVICES)
            .endIpAddress(FirewallRuleEntity.IP_ALLOW_ACCESS_TO_AZURE_SERVICES).build()).commit();
    }

    public void disableAzureAccessRule() {
        Optional.ofNullable(getAzureAccessRule()).ifPresent(PostgreSqlFirewallRule::delete);
    }

    public void disableLocalMachineAccessRule() {
        Optional.ofNullable(getAzureAccessRuleByName(FirewallRuleEntity.getAccessFromLocalFirewallRuleName())).ifPresent(PostgreSqlFirewallRule::delete);
    }

    private PostgreSqlFirewallRule getAzureAccessRule() {
        return getAzureAccessRuleByName(FirewallRuleEntity.ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME);
    }

    private PostgreSqlFirewallRule getAzureAccessRuleByName(String name) {
        return manager.firewallRules().listByServer(serverEntity.getResourceGroupName(), this.serverEntity.getName())
            .stream().filter(e -> StringUtils.equals(name, e.name())).findFirst().map(this::fromFirewallRule).orElse(null);
    }

    public boolean isAzureAccessRuleEnabled() {
        return getAzureAccessRule() != null;
    }

    public boolean isLocalMachineAccessRuleEnabled() {
        return getAzureAccessRuleByName(FirewallRuleEntity.getAccessFromLocalFirewallRuleName()) != null;
    }

    class Creator implements ICommittable<PostgreSqlFirewallRule> {

        private final FirewallRuleConfig config;

        Creator(FirewallRuleConfig config) {
            this.config = config;
        }

        @Override
        public PostgreSqlFirewallRule commit() {
            FirewallRule firewallRule = manager.firewallRules().define(config.getName())
                .withExistingServer(PostgreSqlFirewallRules.this.serverEntity.getResourceGroupName(), PostgreSqlFirewallRules.this.serverEntity.getName())
                .withStartIpAddress(config.getStartIpAddress())
                .withEndIpAddress(config.getEndIpAddress())
                .create();
            return fromFirewallRule(firewallRule);
        }
    }
}
