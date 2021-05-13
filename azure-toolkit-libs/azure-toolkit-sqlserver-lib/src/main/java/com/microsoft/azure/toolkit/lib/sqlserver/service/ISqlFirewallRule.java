package com.microsoft.azure.toolkit.lib.sqlserver.service;

public interface ISqlFirewallRule {

    ISqlFirewallRuleCreator<? extends ISqlFirewallRule> create();

    void delete();

}
