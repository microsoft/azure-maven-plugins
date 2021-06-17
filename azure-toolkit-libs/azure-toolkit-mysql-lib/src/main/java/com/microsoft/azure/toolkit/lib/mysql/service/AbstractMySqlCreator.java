/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.mysql.service;

import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.ICommittable;
import lombok.Getter;

@Getter
public abstract class AbstractMySqlCreator implements ICommittable<MySqlServer>, AzureOperationEvent.Source<MySqlServer> {
    private String name;
    private String resourceGroupName;
    private Region region;
    private String administratorLogin;
    private String administratorLoginPassword;
    private String version;

    public AbstractMySqlCreator withName(String name) {
        this.name = name;
        return this;
    }

    public AbstractMySqlCreator withResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
        return this;
    }

    public AbstractMySqlCreator withRegion(Region region) {
        this.region = region;
        return this;
    }

    public AbstractMySqlCreator withAdministratorLogin(String administratorLogin) {
        this.administratorLogin = administratorLogin;
        return this;
    }

    public AbstractMySqlCreator withAdministratorLoginPassword(String administratorLoginPassword) {
        this.administratorLoginPassword = administratorLoginPassword;
        return this;
    }

    public AbstractMySqlCreator withVersion(String version) {
        this.version = version;
        return this;
    }

}
