/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class AzureConfiguration {
    private String logLevel;
    private String userAgent;
    private String cloud;

    private String installationId;
    private String pluginVersion;
    private String passwordSaveType;
    private Boolean allowTelemetry;
    private String functionCoreToolsPath;

    private InetSocketAddress httpProxy;
}
