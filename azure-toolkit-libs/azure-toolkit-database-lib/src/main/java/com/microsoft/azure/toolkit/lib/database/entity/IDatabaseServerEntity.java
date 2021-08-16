/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;

public interface IDatabaseServerEntity extends IAzureResourceEntity {

    String getResourceGroupName();
    Region getRegion();
    String getAdministratorLoginName();
    String getFullyQualifiedDomainName();
    boolean isEnableAccessFromAzureServices();
    boolean isEnableAccessFromLocalMachine();
    default String getState() {
        return null;
    }

}
