/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database;

import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface IDatabaseServerUpdater<T> extends ICommittable<T> {

    IDatabaseServerUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    IDatabaseServerUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

    @Getter
    abstract class AbstractSqlServerUpdater<T> implements IDatabaseServerUpdater<T> {

        private boolean enableAccessFromAzureServices;
        private boolean enableAccessFromLocalMachine;

        @Override
        public IDatabaseServerUpdater<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices) {
            this.enableAccessFromAzureServices = enableAccessFromAzureServices;
            return this;
        }

        @Override
        public IDatabaseServerUpdater<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine) {
            this.enableAccessFromLocalMachine = enableAccessFromLocalMachine;
            return this;
        }

    }
}
