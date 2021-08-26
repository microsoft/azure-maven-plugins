/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServerEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
public class SqlServerConfig implements IDatabaseServerEntity {

    private final String name;
    private final String id;
    private final ResourceGroup resourceGroup;
    private final Subscription subscription;
    private final Region region;

    private final String administratorLoginName;
    private final String version;
    private final String fullyQualifiedDomainName;
    private final String administratorLoginPassword;
    private final boolean enableAccessFromAzureServices;
    private final boolean enableAccessFromLocalMachine;

    @Override
    public String getSubscriptionId() {
        return Objects.nonNull(subscription) ? subscription.getId() : null;
    }

    @Override
    public String getResourceGroupName() {
        return Objects.nonNull(resourceGroup) ? resourceGroup.getName() : null;
    }

}
