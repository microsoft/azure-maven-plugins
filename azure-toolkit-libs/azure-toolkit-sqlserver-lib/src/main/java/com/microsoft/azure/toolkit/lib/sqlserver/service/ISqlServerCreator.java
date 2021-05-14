/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

public interface ISqlServerCreator<T> extends ICommittable<T> {

    ISqlServerCreator<T> withName(String name);

    ISqlServerCreator<T> withResourceGroup(String resourceGroupName);

    ISqlServerCreator<T> withRegion(Region region);

    ISqlServerCreator<T> withAdministratorLogin(String administratorLogin);

    ISqlServerCreator<T> withAdministratorLoginPassword(String administratorLoginPassword);

    ISqlServerCreator<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices);

    ISqlServerCreator<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine);

    @Getter
    abstract class AbstractSqlServerCreator<T> implements ISqlServerCreator<T> {

        private String name;
        private String resourceGroupName;
        private Region region;
        private String administratorLogin;
        private String administratorLoginPassword;
        private boolean enableAccessFromAzureServices;
        private boolean enableAccessFromLocalMachine;

        @Override
        public ISqlServerCreator<T> withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withResourceGroup(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withRegion(Region region) {
            this.region = region;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withAdministratorLogin(String administratorLogin) {
            this.administratorLogin = administratorLogin;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withAdministratorLoginPassword(String administratorLoginPassword) {
            this.administratorLoginPassword = administratorLoginPassword;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withEnableAccessFromAzureServices(boolean enableAccessFromAzureServices) {
            this.enableAccessFromAzureServices = enableAccessFromAzureServices;
            return this;
        }

        @Override
        public ISqlServerCreator<T> withEnableAccessFromLocalMachine(boolean enableAccessFromLocalMachine) {
            this.enableAccessFromLocalMachine = enableAccessFromLocalMachine;
            return this;
        }

    }

}
