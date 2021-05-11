/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.service;

import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;

public interface ISqlServer {

    SqlServerEntity entity();

    ISqlServerCreator<? extends ISqlServer> create();

    ISqlServerUpdater<? extends ISqlServer> update();

}
