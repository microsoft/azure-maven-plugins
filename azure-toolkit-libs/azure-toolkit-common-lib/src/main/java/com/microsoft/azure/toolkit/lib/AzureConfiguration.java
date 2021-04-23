/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib;

import com.microsoft.rest.LogLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

@Getter
@Setter
public class AzureConfiguration {
    private LogLevel logLevel;
    private String userAgent;
    private String cloud;
    private InetSocketAddress httpProxy;
}
