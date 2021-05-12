/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlServerFirewallUpdater<T> extends ICommittable<T> {

    ISqlServerFirewallUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    ISqlServerFirewallUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

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
