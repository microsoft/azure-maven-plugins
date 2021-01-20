/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

import com.microsoft.azure.AzureEnvironment;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthConfiguration {
    private AuthType type;
    private AzureEnvironment environment;
    private String client;
    private String tenant;
    private String key;
    private String certificate;
    private String certificatePassword;
    private String httpProxyHost;
    private Integer httpProxyPort;
}
