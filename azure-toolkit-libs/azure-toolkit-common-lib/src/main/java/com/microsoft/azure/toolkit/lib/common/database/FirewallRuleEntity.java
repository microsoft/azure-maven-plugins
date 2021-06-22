/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.database;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder(toBuilder = true)
public class FirewallRuleEntity implements IAzureResourceEntity {

    private static final int MAX_FIREWALL_NAME_LENGTH = 128;
    public static final String ACCESS_FROM_AZURE_SERVICES_FIREWALL_RULE_NAME = "AllowAllWindowsAzureIps";
    public static final String IP_ALLOW_ACCESS_TO_AZURE_SERVICES = "0.0.0.0";

    private final String name;
    private final String id;
    private final String subscriptionId;
    private final String startIpAddress;
    private final String endIpAddress;

    public static String getAccessFromLocalFirewallRuleName() {
        final String prefix = "ClientIPAddress_";
        final String suffix = "_" + NetUtils.getMac();
        final int maxHostnameLength = MAX_FIREWALL_NAME_LENGTH - prefix.length() - suffix.length();
        String hostname = NetUtils.getHostName().replaceAll("[^a-zA-Z0-9_-]", StringUtils.EMPTY);
        if (StringUtils.length(hostname) > maxHostnameLength) {
            hostname = StringUtils.substring(hostname, 0, maxHostnameLength);
        }
        return prefix + hostname + suffix;
    }
}
