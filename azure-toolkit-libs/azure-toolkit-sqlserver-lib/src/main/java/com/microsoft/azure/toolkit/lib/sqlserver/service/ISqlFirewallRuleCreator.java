/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlFirewallRuleCreator<T> extends ICommittable<T> {

    ISqlFirewallRuleCreator<T> withName(String name);

    ISqlFirewallRuleCreator<T> wihStartIpAddress(String startIpAddress);

    ISqlFirewallRuleCreator<T> withEndIpAddress(String endIpAddress);

    @Getter
    abstract class AbstractSqlFirewallRuleCreator<T> implements ISqlFirewallRuleCreator<T> {

        private String name;
        private String startIpAddress;
        private String endIpAddress;

        @Override
        public ISqlFirewallRuleCreator<T> withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ISqlFirewallRuleCreator<T> wihStartIpAddress(String startIpAddress) {
            this.startIpAddress = startIpAddress;
            return this;
        }

        @Override
        public ISqlFirewallRuleCreator<T> withEndIpAddress(String endIpAddress) {
            this.endIpAddress = endIpAddress;
            return this;
        }
    }
}
