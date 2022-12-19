/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface IDatabaseServer<T extends IDatabase> extends AzResourceBase {
    @Nullable
    Region getRegion();

    @Nullable
    String getAdminName();

    @Nullable
    default String getFullAdminName() {
        return String.format("%s@%s", this.getAdminName(), this.getName());
    }

    @Nullable
    String getFullyQualifiedDomainName();

    boolean isAzureServiceAccessAllowed();

    boolean isLocalMachineAccessAllowed();

    @Nullable
    String getVersion();

    @Nonnull
    String getStatus();

    @Nullable
    String getType();

    @Nonnull
    String getLocalMachinePublicIp();

    @Nonnull
    JdbcUrl getJdbcUrl();

    @Nonnull
    List<T> listDatabases();
}
