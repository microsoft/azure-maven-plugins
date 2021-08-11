/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureConfiguration {
    private String logLevel;
    private String userAgent;
    private String cloud;
    private String machineId;
    private String product;
    private String version;
    private String databasePasswordSaveType;
    private Boolean telemetryEnabled; // null means true
    private String functionCoreToolsPath;
    private String proxySource;
    private String httpProxyHost;
    private int httpProxyPort;
    private String proxyUsername;
    private String proxyPassword;
}
