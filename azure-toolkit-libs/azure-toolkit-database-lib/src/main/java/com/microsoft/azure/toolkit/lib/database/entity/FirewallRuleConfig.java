package com.microsoft.azure.toolkit.lib.database.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FirewallRuleConfig {

    private String name;
    private String startIpAddress;
    private String endIpAddress;

}
