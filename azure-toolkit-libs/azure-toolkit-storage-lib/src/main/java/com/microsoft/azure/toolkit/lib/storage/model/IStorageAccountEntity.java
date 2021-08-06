/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;

public interface IStorageAccountEntity extends IAzureResourceEntity {

    Region getRegion();
    String getResourceGroupName();
    Performance getPerformance();
    Redundancy getRedundancy();
    Kind getKind();

}
