/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import lombok.Data;

@Data
public class AppRawConfig {
    private String subscriptionId;
    private String clusterName;
    private String appName;
    private String isPublic;
    private AppDeploymentRawConfig deployment;
}
