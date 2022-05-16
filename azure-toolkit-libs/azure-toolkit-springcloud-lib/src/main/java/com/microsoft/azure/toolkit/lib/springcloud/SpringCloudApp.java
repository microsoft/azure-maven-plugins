/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class SpringCloudApp extends AbstractAzResource<SpringCloudApp, SpringCloudCluster, SpringApp>
    implements Startable, Deletable {

    @Nonnull
    private final SpringCloudDeploymentModule deploymentModule;
    @Nullable
    private AtomicReference<String> activeDeploymentName = null;

    protected SpringCloudApp(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.deploymentModule = new SpringCloudDeploymentModule(this);
    }

    /**
     * copy constructor
     */
    protected SpringCloudApp(@Nonnull SpringCloudApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
        this.activeDeploymentName = origin.activeDeploymentName;
    }

    protected SpringCloudApp(@Nonnull SpringApp remote, @Nonnull SpringCloudAppModule module) {
        super(remote.name(), module);
        this.deploymentModule = new SpringCloudDeploymentModule(this);
        this.setRemote(remote);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, SpringCloudApp, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringApp remote) {
        final SpringCloudDeployment activeDeployment = this.getActiveDeployment();
        if (Objects.isNull(activeDeployment)) {
            return Status.INACTIVE;
        }
        activeDeployment.refresh();
        return activeDeployment.getStatusSync();
    }

    @Nonnull
    public SpringCloudDeploymentModule deployments() {
        return this.deploymentModule;
    }

    // MODIFY
    @AzureOperation(name = "resource.start_resource.resource", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).start(), Status.STARTING);
    }

    @AzureOperation(name = "resource.stop_resource.resource", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).stop(), Status.STOPPING);
    }

    @AzureOperation(name = "resource.restart_resource.resource", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).restart(), Status.RESTARTING);
    }

    // READ
    public boolean isPublicEndpointEnabled() {
        if (Objects.nonNull(this.getRemote())) {
            return this.getRemote().isPublic();
        }
        return false;
    }

    @Override
    protected void setRemote(@Nullable SpringApp newRemote) {
        super.setRemote(newRemote);
        this.activeDeploymentName = null;
    }

    @Nullable
    public synchronized String getActiveDeploymentName() {
        if (activeDeploymentName == null) {
            activeDeploymentName = new AtomicReference<>(Optional.ofNullable(this.getRemote()).map(SpringApp::activeDeploymentName).orElse(null));
        }
        return activeDeploymentName.get();
    }

    @Nullable
    public SpringCloudDeployment getActiveDeployment() {
        return Optional.ofNullable(this.getActiveDeploymentName()).map(n -> this.deployments().get(n, this.getResourceGroupName())).orElse(null);
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
            return String.format("%s/%s/%s", endpoint, this.getName(), d);
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

    public boolean isPersistentDiskEnabled() {
        return Objects.nonNull(this.getPersistentDisk());
    }
}
