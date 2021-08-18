/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;

import java.util.List;

public interface IDatabaseServer {

    IDatabaseServerEntity entity();

    List<? extends IAzureResourceEntity> databases();

}
