/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

@Getter
@Builder
public class SpringCloudAppConfig {

    private final String subscriptionId;
    private final String clusterName;
    private final String appName;
    private final String resourceGroup;
    private final Boolean isPublic;
    private final String runtimeVersion;
    private final String activeDeploymentName;
    private final SpringCloudDeploymentConfig deployment;

    public Boolean isPublic() {
        return BooleanUtils.isTrue(isPublic);
    }
}
