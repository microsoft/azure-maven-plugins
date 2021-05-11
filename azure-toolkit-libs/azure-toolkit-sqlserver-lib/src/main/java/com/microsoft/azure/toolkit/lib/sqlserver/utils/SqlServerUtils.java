/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.sqlserver.utils;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;

public class SqlServerUtils {

    public static SqlServerEntity fromSqlServer(SqlServer server) {
        return SqlServerEntity.builder().name(server.name())
                .id(server.id())
                .region(Region.fromName(server.regionName()))
                .resourceGroup(server.resourceGroupName())
                .subscriptionId(ResourceId.fromString(server.id()).subscriptionId())
                .kind(server.kind())
                .administratorLogin(server.administratorLogin())
                .version(server.version())
                .state(server.state())
                .fullyQualifiedDomainName(server.fullyQualifiedDomainName())
                .type(server.type())
                .build();
    }

}
