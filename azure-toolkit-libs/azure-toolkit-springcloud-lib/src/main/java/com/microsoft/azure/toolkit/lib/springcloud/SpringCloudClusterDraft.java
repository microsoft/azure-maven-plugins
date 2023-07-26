/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.Region;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.models.BuilderProvisioningState;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentDraft.Draft;

@Slf4j
public class SpringCloudClusterDraft extends SpringCloudCluster implements Draft<SpringCloudCluster, SpringService> {

    @Getter
    @Nullable
    private final SpringCloudCluster origin;
    @Setter
    @Nullable
    private Config config;

    SpringCloudClusterDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SpringCloudClusterModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    SpringCloudClusterDraft(@Nonnull SpringCloudCluster origin) {
        super(origin);
        this.origin = origin;
    }

    public SpringCloudClusterDraft withConfig(Config config) {
        this.setConfig(config);
        return this;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.create_service.service", params = {"this.getName()"})
    public SpringService createResourceInAzure() {
        final IAzureMessager messager = AzureMessager.getMessager();
        final ResourceGroup rg = Objects.requireNonNull(this.getResourceGroup());
        if (rg.isDraftForCreating()) {
            ((ResourceGroupDraft) rg).createIfNotExist();
        }
        final String rgName = this.getResourceGroupName();
        final Region region = Objects.requireNonNull(this.getRegion(), "'Region' is required.");
        final String serviceName = this.getName();
        final AppPlatformManager manager = Objects.requireNonNull(this.getParent().getRemote());
        messager.info(AzureString.format("Start creating Spring apps ({0})...", serviceName));

        manager.springServices()
            .define(this.getName())
            .withRegion(region)
            .withExistingResourceGroup(rgName)
            .withSku(Optional.ofNullable(this.getSku()).map(Sku::toSku).orElse(null))
            .create();

        // wait until builder ready
        BuilderProvisioningState provisioningState = manager.serviceClient().getBuildServiceBuilders().get(rgName, serviceName, "default", "default").properties().provisioningState();
        while (provisioningState != BuilderProvisioningState.SUCCEEDED) {
            provisioningState = manager.serviceClient().getBuildServiceBuilders().get(rgName, serviceName, "default", "default").properties().provisioningState();
            ResourceManagerUtils.sleep(Duration.ofSeconds(5));
            log.debug("Waiting for builder ready...");
        }

        messager.success(AzureString.format("Spring apps ({0}) is successfully created.", serviceName));
        return manager.springServices().getByResourceGroup(rgName, this.getName());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.update_app.app", params = {"this.getName()"})
    public SpringService updateResourceInAzure(@Nonnull SpringService service) {
        throw new NotImplementedException("Update Spring Cloud Cluster is not supported yet");
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * set name of the service.
     * WARNING: only work for creation
     */
    public void setName(@Nonnull String name) {
        this.ensureConfig().setName(name);
    }

    @Nonnull
    @Override
    public String getName() {
        return Optional.ofNullable(config).map(Config::getName).orElseGet(super::getName);
    }

    @Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion);
    }

    @Nullable
    @Override
    public Sku getSku() {
        return Optional.ofNullable(config).map(Config::getSku).orElseGet(super::getSku);
    }

    @Nullable
    @Override
    public ResourceGroup getResourceGroup() {
        return Optional.ofNullable(config).map(Config::getResourceGroup).orElseGet(super::getResourceGroup);
    }

    @Nullable
    @Override
    public String getManagedEnvironmentId() {
        return Optional.ofNullable(config).map(Config::getManagedEnvironmentId).orElseGet(super::getManagedEnvironmentId);
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    @Accessors(chain = true)
    public static class Config {
        private String name;
        @Nullable
        private Region region;
        @Nullable
        private Sku sku;
        @Nullable
        private String managedEnvironmentId;
        @Nullable
        private ResourceGroup resourceGroup;
    }
}