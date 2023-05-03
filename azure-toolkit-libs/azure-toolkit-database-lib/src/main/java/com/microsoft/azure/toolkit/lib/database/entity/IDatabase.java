/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IDatabase extends AzResource {
    @Nullable
    String getCollation();

    @Nonnull
    IDatabaseServer<? extends IDatabase> getServer();

    @Nonnull
    JdbcUrl getJdbcUrl();
}
