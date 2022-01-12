/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.entity;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public interface IFirewallRule extends AzResourceBase {
    int MAX_FIREWALL_NAME_LENGTH = 128;
    String AZURE_SERVICES_ACCESS_FIREWALL_RULE_NAME = "AllowAllWindowsAzureIps";
    String IP_ALLOW_ACCESS_TO_AZURE_SERVICES = "0.0.0.0";

    @Nullable
    String getStartIpAddress();

    @Nullable
    String getEndIpAddress();

    static String getLocalMachineAccessRuleName() {
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
