/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql.service;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.FirewallRule;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlEntity;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MySqlFirewallRules {
    private static final String NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL = "ClientIPAddress_";
    private static final String NAME_ALLOW_ACCESS_TO_AZURE_SERVICES = "AllowAllWindowsAzureIps";
    private static final String IP_ALLOW_ACCESS_TO_AZURE_SERVICES = "0.0.0.0";

    private final MySqlManager manager;
    private final MySqlEntity mySqlEntity;

    public List<MySqlFirewallRule> list() {
        return manager.firewallRules().listByServer(mySqlEntity.getResourceGroup(), this.mySqlEntity.getName()).stream()
            .map(this::fromFirewallRule).collect(Collectors.toList());
    }

    private MySqlFirewallRule fromFirewallRule(FirewallRule firewallRule) {
        return new MySqlFirewallRule(manager, mySqlEntity, MySqlFirewallRule.fromFirewallRule(firewallRule));
    }

    public AbstractMySqlFirewallRuleCreator create() {
        return new MySqlFirewallRuleCreator();
    }

    public MySqlFirewallRule enableLocalMachineAccessRule(String publicIp) {
        Preconditions.checkArgument(StringUtils.isNotBlank(publicIp),
            "Cannot enable local machine access to mysql server due to error: cannot get public ip.");
        String name = getAccessFromLocalRuleName();
        MySqlFirewallRule myFirewallRule = getAzureAccessRuleByName(name);
        if (myFirewallRule != null) {
            if (myFirewallRule.getEntity() != null && StringUtils.equals(myFirewallRule.getEntity().getStartIpAddress(), publicIp)) {
                return myFirewallRule;
            }
            return myFirewallRule.update(publicIp, publicIp).commit();
        } else {
            return create().withName(name)
                .wihStartIpAddress(publicIp)
                .withEndIpAddress(publicIp).commit();
        }
    }

    public MySqlFirewallRule enableAzureAccessRule() {
        MySqlFirewallRule allowAzureAccessRule = getAzureAccessRule();
        if (allowAzureAccessRule != null) {
            return allowAzureAccessRule;
        }
        return create().withName(NAME_ALLOW_ACCESS_TO_AZURE_SERVICES)
            .wihStartIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES)
            .withEndIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES).commit();
    }

    public void disableAzureAccessRule() {
        Optional.ofNullable(getAzureAccessRule()).ifPresent(MySqlFirewallRule::delete);
    }

    public void disableLocalMachineAccessRule() {
        Optional.ofNullable(getAzureAccessRuleByName(getAccessFromLocalRuleName())).ifPresent(MySqlFirewallRule::delete);
    }

    private MySqlFirewallRule getAzureAccessRule() {
        return getAzureAccessRuleByName(NAME_ALLOW_ACCESS_TO_AZURE_SERVICES);
    }

    private String getAccessFromLocalRuleName() {
        return NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL + NetUtils.getHostName() + "_" + NetUtils.getMac();
    }

    private MySqlFirewallRule getAzureAccessRuleByName(String name) {
        return manager.firewallRules().listByServer(mySqlEntity.getResourceGroup(), this.mySqlEntity.getName())
            .stream().filter(e -> StringUtils.equals(name, e.name())).findFirst().map(this::fromFirewallRule).orElse(null);
    }

    public boolean isAzureAccessRuleEnabled() {
        return getAzureAccessRule() != null;
    }

    public boolean isLocalMachineAccessRuleEnabled() {
        return getAzureAccessRuleByName(getAccessFromLocalRuleName()) != null;
    }

    class MySqlFirewallRuleCreator extends AbstractMySqlFirewallRuleCreator {
        public MySqlFirewallRule commit() {
            FirewallRule firewallRule = manager.firewallRules().define(this.getName()).withExistingServer(
                MySqlFirewallRules.this.mySqlEntity.getResourceGroup(), MySqlFirewallRules.this.mySqlEntity.getName()).withStartIpAddress(getStartIpAddress())
                .withEndIpAddress(getEndIpAddress()).create();
            return fromFirewallRule(firewallRule);
        }
    }
}
