package com.microsoft.azure.toolkit.lib.sqlserver.service;

public interface ISqlServerFirewall {

    ISqlServerFirewallCreator<? extends ISqlServerFirewall> create();

}
