/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MavenAuthConfiguration {
    private String authType;
    private String client;
    private String tenant;
    private String key;
    private String certificate;
    private String certificatePassword;
    private String environment;
    private String serverId;
    private String httpProxyHost;
    private String httpProxyPort;

}
