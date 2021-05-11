/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import lombok.Getter;

public interface ISqlServerUpdater<T> {

    ISqlServerUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    ISqlServerUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

    T commit();

    @Getter
    abstract class AbstractSqlServerUpdater<T> implements ISqlServerUpdater<T> {

        private boolean enableAccessFromAzureServices;
        private boolean enableAccessFromLocalMachine;

        @Override
        public ISqlServerUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices) {
            this.enableAccessFromAzureServices = enableAccessFromAzureServices;
            return this;
        }

        @Override
        public ISqlServerUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine) {
            this.enableAccessFromLocalMachine = this.enableAccessFromLocalMachine;
            return this;
        }
    }
}
