/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder(toBuilder = true)
public class SqlServerEntity implements IAzureResourceEntity {

    private String name;
    private String id;
    private String resourceGroup;
    private String subscriptionId;
    private Region region;

    private String kind;
    private String administratorLogin;
    private String administratorLoginPassword;
    private String version;
    private String state;
    private String fullyQualifiedDomainName;
    private String type;

    private List<Object> sqlFirewallRules;
}
