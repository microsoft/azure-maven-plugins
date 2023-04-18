/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.microsoft.azure.toolkit.lib.common.model.*;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import com.microsoft.azure.toolkit.lib.servicelinker.Consumer;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudPersistentDisk;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Getter
public class SpringCloudApp extends AbstractAzResource<SpringCloudApp, SpringCloudCluster, SpringApp>
    implements Startable, Deletable, Consumer {

    @Nonnull
    private final SpringCloudDeploymentModule deploymentModule;
    private final ServiceLinkerModule linkerModule;
    @Nullable
    private SpringCloudDeployment activeDeployment = null;

    protected SpringCloudApp(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.deploymentModule = new SpringCloudDeploymentModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    /**
     * copy constructor
     */
    protected SpringCloudApp(@Nonnull SpringCloudApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
        this.activeDeployment = origin.activeDeployment;
        this.linkerModule = origin.linkerModule;
    }

    protected SpringCloudApp(@Nonnull SpringApp remote, @Nonnull SpringCloudAppModule module) {
        super(remote.name(), module);
        this.deploymentModule = new SpringCloudDeploymentModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    @Override
    public void invalidateCache() {
        super.invalidateCache();
        this.activeDeployment = null;
    }

    @Override
    protected void updateAdditionalProperties(final SpringApp newRemote, final SpringApp oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.activeDeployment = Optional.ofNullable(newRemote).map(SpringApp::activeDeploymentName)
            .map(name -> this.deployments().get(name, this.getResourceGroupName())).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(deploymentModule, linkerModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringApp remote) {
        final SpringCloudDeployment activeDeployment = this.getActiveDeployment();
        if (Objects.isNull(activeDeployment)) {
            return Status.INACTIVE;
        }
        return activeDeployment.getStatus();
    }

    @Nonnull
    public SpringCloudDeploymentModule deployments() {
        return this.deploymentModule;
    }

    // MODIFY
    @AzureOperation(name = "azure/resource.start_resource.resource", params = {"this.name()"})
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).start(), Status.STARTING);
        this.refresh();
    }

    @AzureOperation(name = "azure/resource.stop_resource.resource", params = {"this.name()"})
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).stop(), Status.STOPPING);
        this.refresh();
    }

    @AzureOperation(name = "azure/resource.restart_resource.resource", params = {"this.name()"})
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getActiveDeployment()).restart(), Status.RESTARTING);
        this.refresh();
    }

    // READ
    public boolean isPublicEndpointEnabled() {
        if (Objects.nonNull(this.getRemote())) {
            return this.getRemote().isPublic();
        }
        return false;
    }

    @Nullable
    public synchronized String getActiveDeploymentName() {
        return Optional.ofNullable(this.getActiveDeployment()).map(AbstractAzResource::getName).orElse(null);
    }

    @Nullable
    public SpringCloudDeployment getActiveDeployment() {
        return this.remoteOptional(true).map(r -> this.activeDeployment).orElse(null);
    }

    @Nullable
    public SpringCloudDeployment getCachedActiveDeployment() {
        return this.activeDeployment;
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
