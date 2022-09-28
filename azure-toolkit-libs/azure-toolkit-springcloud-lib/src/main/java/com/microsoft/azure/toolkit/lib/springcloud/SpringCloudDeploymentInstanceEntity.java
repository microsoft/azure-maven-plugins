/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.DeploymentInstance;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class SpringCloudDeploymentInstanceEntity {
    // https://${fqdn}/api/remoteDebugging/apps/${appName}/deployments/${deploymentName}/instances/${instanceName}
    private static final String REMOTE_URL_TEMPLATE = "https://%s/api/remoteDebugging/apps/%s/deployments/%s/instances/%s";
    @Nonnull
    private final SpringCloudDeployment deployment;
    @Nonnull
    private final String name;
    @Nonnull
    @JsonIgnore
    @Getter(AccessLevel.PACKAGE)
    private final transient DeploymentInstance remote;

    SpringCloudDeploymentInstanceEntity(@Nonnull DeploymentInstance remote, @Nonnull SpringCloudDeployment deployment) {
        this.remote = remote;
        this.name = remote.name();
        this.deployment = deployment;
    }

    @Nonnull
    public String status() {
        return this.remote.status();
    }

    @Nonnull
    public String reason() {
        return this.remote.reason();
    }

    @Nonnull
    public String discoveryStatus() {
        return this.remote.discoveryStatus();
    }

    public String getRemoteUrl() {
        SpringCloudApp app = this.getDeployment().getParent();
        return String.format(REMOTE_URL_TEMPLATE, app.getParent().getFqdn(), app.getName(), this.getDeployment().getName(), this.name);
    }
}
