/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlServerUpdater<T> extends ICommittable<T> {

    ISqlServerUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    ISqlServerUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

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
            this.enableAccessFromLocalMachine = enableAccessFromLocalMachine;
            return this;
        }

    }
}
