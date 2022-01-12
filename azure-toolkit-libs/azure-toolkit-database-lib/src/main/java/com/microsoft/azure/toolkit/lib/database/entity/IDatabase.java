/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;

public interface IDatabase extends AzResourceBase {
    String getCollation();

    IDatabaseServer<? extends IDatabase> getServer();

    JdbcUrl getJdbcUrl();
}
