/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre.model;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.entity.AbstractAzureResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
@EqualsAndHashCode
public class PostgreSqlDatabaseEntity extends AbstractAzureResource.RemoteAwareResourceEntity<com.azure.resourcemanager.postgresql.models.Database> {
    @Nonnull
    private final ResourceId resourceId;
    @Nonnull final PostgreSqlManager manager;

    public PostgreSqlDatabaseEntity(PostgreSqlManager manager, com.azure.resourcemanager.postgresql.models.Database database) {
        this.resourceId = ResourceId.fromString(database.id());
        this.manager = manager;
        this.remote = database;
    }

    @Override
    public String getSubscriptionId() {
        return resourceId.subscriptionId();
    }

    public String getId() {
        return resourceId.id();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    public String getResourceGroupName() {
        return resourceId.resourceGroupName();
    }
}
