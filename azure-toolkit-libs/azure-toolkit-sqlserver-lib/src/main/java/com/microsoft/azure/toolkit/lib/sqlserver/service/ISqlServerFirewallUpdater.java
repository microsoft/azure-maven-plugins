/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import lombok.Getter;

public interface ISqlServerFirewallUpdater<T> {

    ISqlServerFirewallUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    ISqlServerFirewallUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

    T commit();

    @Getter
    abstract class AbstractSqlServerFirewallUpdater<T> implements ISqlServerFirewallUpdater<T> {

        private boolean enableAccessFromAzureServices;
        private boolean enableAccessFromLocalMachine;

        @Override
        public ISqlServerFirewallUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices) {
            this.enableAccessFromAzureServices = enableAccessFromAzureServices;
            return this;
        }

        @Override
        public ISqlServerFirewallUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine) {
            this.enableAccessFromLocalMachine = enableAccessFromLocalMachine;
            return this;
        }

    }
}
