/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver.model;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class SqlFirewallRuleEntity implements IAzureResourceEntity {

    public static final String ACCESS_FROM_LOCAL_FIREWALL_RULE_NAME = "ClientIPAddress_" + NetUtils.getHostName() + "_" + NetUtils.getMac();
    public static final String ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME = "AllowAllWindowsAzureIps";

    private String name;
    private String id;
    private String subscriptionId;
    private String startIpAddress;
    private String endIpAddress;
}
