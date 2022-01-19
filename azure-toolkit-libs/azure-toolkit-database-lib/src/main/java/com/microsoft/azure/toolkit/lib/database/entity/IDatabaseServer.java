/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;

import javax.annotation.Nullable;
import java.util.List;

public interface IDatabaseServer<T extends IDatabase> extends AzResourceBase {
    @Nullable
    Region getRegion();

    @Nullable
    String getAdminName();

    @Nullable
    String getFullyQualifiedDomainName();

    boolean isAzureServiceAccessAllowed();

    boolean isLocalMachineAccessAllowed();

    @Nullable
    String getVersion();

    @Nullable
    String getStatus();

    @Nullable
    String getType();

    String getLocalMachinePublicIp();

    JdbcUrl getJdbcUrl();

    List<T> listDatabases();
}
