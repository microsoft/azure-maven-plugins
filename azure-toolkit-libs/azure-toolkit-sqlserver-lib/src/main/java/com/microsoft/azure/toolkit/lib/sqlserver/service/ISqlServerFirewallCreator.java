/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlServerFirewallCreator<T> extends ICommittable<T> {

    ISqlServerFirewallCreator<T> withName(String name);

    ISqlServerFirewallCreator<T> wihStartIpAddress(String startIpAddress);

    ISqlServerFirewallCreator<T> withEndIpAddress(String endIpAddress);

    @Getter
    abstract class AbstractSqlServerFirewallCreator<T> implements ISqlServerFirewallCreator<T> {

        private String name;
        private String startIpAddress;
        private String endIpAddress;

        @Override
        public ISqlServerFirewallCreator<T> withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ISqlServerFirewallCreator<T> wihStartIpAddress(String startIpAddress) {
            this.startIpAddress = startIpAddress;
            return this;
        }

        @Override
        public ISqlServerFirewallCreator<T> withEndIpAddress(String endIpAddress) {
            this.endIpAddress = endIpAddress;
            return this;
        }
    }
}
