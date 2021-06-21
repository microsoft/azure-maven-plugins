/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

@Getter
public abstract class AbstractMySqlFirewallRuleCreator implements ICommittable<MySqlFirewallRule> {
    private String name;
    private String startIpAddress;
    private String endIpAddress;

    public AbstractMySqlFirewallRuleCreator withName(String name) {
        this.name = name;
        return this;
    }

    public AbstractMySqlFirewallRuleCreator wihStartIpAddress(String startIpAddress) {
        this.startIpAddress = startIpAddress;
        return this;
    }

    public AbstractMySqlFirewallRuleCreator withEndIpAddress(String endIpAddress) {
        this.endIpAddress = endIpAddress;
        return this;
    }
}
