/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class SqlServerFirewallEntity implements IAzureResourceEntity {

    private String name;
    private String id;
    private String subscriptionId;
    private String startIpAddress;
    private String endIpAddress;
}
