/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@Getter
public class SpringCloudApp extends AbstractAzResource<SpringCloudApp, SpringCloudCluster, SpringApp> {

    @Nonnull
    private final SpringCloudDeploymentModule deploymentModule;

    protected SpringCloudApp(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module.getParent().getResourceGroup(), module);
        this.deploymentModule = new SpringCloudDeploymentModule(this);
    }

    protected SpringCloudApp(@Nonnull SpringApp remote, @Nonnull SpringCloudAppModule module) {
        this(remote.name(), module);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringApp remote) {
        final SpringCloudDeployment active = this.getActiveDeployment();
        if (Objects.isNull(active)) {
            return Status.INACTIVE;
        }
        return active.getStatus();
    }

    @Nonnull
    public SpringCloudDeploymentModule deployments() {
        return this.deploymentModule;
    }

    // MODIFY
    @AzureOperation(name = "springcloud.start_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        this.doModify(() -> this.getRemote().getActiveDeployment().start());
    }

    @AzureOperation(name = "springcloud.stop_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        this.doModify(() -> this.getRemote().getActiveDeployment().stop());
    }

    @AzureOperation(name = "springcloud.restart_app.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> this.getRemote().getActiveDeployment().restart());
    }

    // READ
    public boolean isPublic() {
        if (Objects.nonNull(this.getRemote())) {
            return this.getRemote().isPublic();
        }
        return false;
    }

    @Nullable
    public String getActiveDeploymentName() {
        return Optional.ofNullable(this.getRemote()).map(SpringApp::activeDeploymentName).orElse(null);
    }

    @Nullable
    public SpringCloudDeployment getActiveDeployment() {
        return Optional.ofNullable(this.getActiveDeploymentName()).map(n -> this.deployments().get(n, this.getResourceGroup())).orElse(null);
    }

    @Nullable
    public String getApplicationUrl() {
        final String url = Optional.ofNullable(this.getRemote()).map(SpringApp::url).orElse(null);
        return StringUtils.isBlank(url) || url.equalsIgnoreCase("None") ? null : url;
    }

    @Nullable
    public String getTestUrl() {
        return Optional.ofNullable(this.getRemote()).map(SpringApp::activeDeploymentName).map(d -> {
            final String endpoint = this.getRemote().parent().listTestKeys().primaryTestEndpoint();
            return String.format("%s/%s/%s/", endpoint, this.getName(), d);
        }).orElse(null);
    }

    @Nullable
    public String getLogStreamingEndpoint(String instanceName) {
        return Optional.ofNullable(this.getRemote()).map(SpringApp::activeDeploymentName).map(d -> {
            final String endpoint = this.getRemote().parent().listTestKeys().primaryTestEndpoint();
            return String.format("%s/api/logstream/apps/%s/instances/%s", endpoint.replace(".test", ""), this.getName(), instanceName);
        }).orElse(null);
    }

    @Nullable
    public SpringCloudPersistentDisk getPersistentDisk() {
        final PersistentDisk disk = Optional.ofNullable(this.getRemote()).map(SpringApp::persistentDisk).orElse(null);
        return Optional.ofNullable(disk).filter(d -> d.sizeInGB() > 0)
            .map(d -> SpringCloudPersistentDisk.builder()
                .sizeInGB(disk.sizeInGB())
                .mountPath(disk.mountPath())
                .usedInGB(disk.usedInGB()).build())
            .orElse(null);
    }
}
