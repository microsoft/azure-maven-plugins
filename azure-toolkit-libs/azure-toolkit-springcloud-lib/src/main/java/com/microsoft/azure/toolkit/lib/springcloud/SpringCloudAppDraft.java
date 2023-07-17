/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.fluent.models.AppResourceInner;
import com.azure.resourcemanager.appplatform.models.ActiveDeploymentCollection;
import com.azure.resourcemanager.appplatform.models.AppResourceProperties;
import com.azure.resourcemanager.appplatform.models.PersistentDisk;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.TemporaryDisk;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudClusterConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppDraft extends SpringCloudApp implements AzResource.Draft<SpringCloudApp, SpringApp> {
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    public static final String DEFAULT_DISK_MOUNT_PATH = "/persistent";
    public static final String DEFAULT_DEPLOYMENT_NAME = "default";
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Apps</a>
     */
    public static final int BASIC_TIER_DEFAULT_DISK_SIZE = 1;
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Apps</a>
     */
    public static final int STANDARD_TIER_DEFAULT_DISK_SIZE = 50;
    public static final int DEFAULT_TEMP_DISK_SIZE = 5;
    public static final String DEFAULT_TEMP_DISK_MOUNT_PATH = "/tmp";
    @Getter
    @Nullable
    private final SpringCloudApp origin;
    @Nullable
    private SpringCloudDeployment activeDeployment;
    @Nullable
    private Config config;

    SpringCloudAppDraft(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.origin = null;
    }

    SpringCloudAppDraft(@Nonnull SpringCloudApp origin) {
        super(origin);
        this.origin = origin;
    }

    public void setConfig(@Nonnull SpringCloudAppConfig c) {
        this.setName(c.getAppName());
        this.setActiveDeploymentName(c.getActiveDeploymentName());
        this.setPublicEndpointEnabled(c.getIsPublic());
        final SpringCloudDeploymentConfig deploymentConfig = c.getDeployment();
        final SpringCloudDeploymentDraft deploymentDraft = this.updateOrCreateActiveDeployment();
        this.setPersistentDiskEnabled(deploymentConfig.getEnablePersistentStorage());
        deploymentDraft.setConfig(deploymentConfig);
    }

    @Nonnull
    public SpringCloudAppConfig getConfig() {
        final SpringCloudDeploymentConfig deploymentConfig = activeDeployment instanceof SpringCloudDeploymentDraft ?
            ((SpringCloudDeploymentDraft) activeDeployment).getConfig() : SpringCloudDeploymentConfig.builder().build();
        deploymentConfig.setEnablePersistentStorage(this.isPersistentDiskEnabled());
        return SpringCloudAppConfig.builder()
            .cluster(SpringCloudClusterConfig.fromCluster(this.getParent()))
            .appName(this.getName())
            .isPublic(this.isPublicEndpointEnabled())
            .activeDeploymentName(this.getActiveDeploymentName())
            .deployment(deploymentConfig)
            .build();
    }

    @Override
    public void invalidateCache() {
        super.invalidateCache();
        this.reset();
    }

    @Override
    public void reset() {
        this.config = null;
        this.activeDeployment = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.create_app.app", params = {"this.getName()"})
    public SpringApp createResourceInAzure() {
        final String appName = this.getName();
        final SpringService service = Objects.requireNonNull(this.getParent().getRemote());
        final boolean newPublicEndpointEnabled = this.isPublicEndpointEnabled();
        final Integer newDiskSize = this.isPersistentDiskEnabled() ? this.getParent().isStandardTier() ? STANDARD_TIER_DEFAULT_DISK_SIZE : BASIC_TIER_DEFAULT_DISK_SIZE : null;
        final PersistentDisk newDisk = this.isPersistentDiskEnabled() ? new PersistentDisk().withSizeInGB(newDiskSize).withMountPath(DEFAULT_DISK_MOUNT_PATH) : null;
        final TemporaryDisk tmpDisk = this.getParent().isEnterpriseTier() ? null : new TemporaryDisk().withSizeInGB(DEFAULT_TEMP_DISK_SIZE).withMountPath(DEFAULT_TEMP_DISK_MOUNT_PATH);

        final AppResourceInner appResource = new AppResourceInner()
            .withProperties(new AppResourceProperties()
                .withPersistentDisk(newDisk)
                .withTemporaryDisk(tmpDisk)
                .withPublicProperty(newPublicEndpointEnabled));

        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating app({0})...", appName));
        service.manager().serviceClient().getApps().createOrUpdate(this.getResourceGroupName(), service.name(), appName, appResource);
        messager.success(AzureString.format("App({0}) is successfully created.", appName));
        return service.apps().getByName(appName);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/springcloud.update_app.app", params = {"this.getName()"})
    public SpringApp updateResourceInAzure(@Nonnull SpringApp origin) {
        if (this.isModified()) {
            final String oldActiveDeploymentName = super.getActiveDeploymentName();
            final String newActiveDeploymentName = this.getActiveDeploymentName();
            final boolean newPublicEndpointEnabled = this.isPublicEndpointEnabled();
            final Integer newDiskSize = this.isPersistentDiskEnabled() ? this.getParent().isStandardTier() ? STANDARD_TIER_DEFAULT_DISK_SIZE : BASIC_TIER_DEFAULT_DISK_SIZE : null;
            final PersistentDisk newDisk = this.isPersistentDiskEnabled() ? new PersistentDisk().withSizeInGB(newDiskSize).withMountPath(DEFAULT_DISK_MOUNT_PATH) :
                new PersistentDisk().withSizeInGB(0).withMountPath(DEFAULT_DISK_MOUNT_PATH);

            final AppResourceInner appResource = new AppResourceInner()
                .withProperties(new AppResourceProperties()
                    .withPersistentDisk(newDisk)
                    .withPublicProperty(newPublicEndpointEnabled));

            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating app({0})...", origin.name()));
            final SpringService service = origin.parent();
            if (!Objects.equals(super.isPublicEndpointEnabled(), newPublicEndpointEnabled) ||
                !Objects.equals(super.isPersistentDiskEnabled(), this.isPersistentDiskEnabled())) {
                service.manager().serviceClient().getApps().createOrUpdate(this.getResourceGroupName(), service.name(), origin.name(), appResource);
            }
            if (!Objects.equals(oldActiveDeploymentName, newActiveDeploymentName) && StringUtils.isNotBlank(newActiveDeploymentName)) {
                service.manager().serviceClient().getApps().setActiveDeployments(this.getResourceGroupName(), service.name(), origin.name(), new ActiveDeploymentCollection()
                    .withActiveDeploymentNames(Collections.singletonList(newActiveDeploymentName)));
            }
            messager.success(AzureString.format("App({0}) is successfully updated.", origin.name()));
            messager.warning(UPDATE_APP_WARNING);
            return service.apps().getByName(origin.name());
        }
        return origin;
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    /**
     * set name of the app.
     * WARNING: only work for creation
     */
    public void setName(@Nonnull String name) {
        this.ensureConfig().setName(name);
    }

    @Nonnull
    public String getName() {
        return Optional.ofNullable(config).map(Config::getName).orElseGet(super::getName);
    }

    public void setPublicEndpointEnabled(Boolean enabled) {
        this.ensureConfig().setPublicEndpointEnabled(enabled);
    }

    public boolean isPublicEndpointEnabled() {
        return Optional.ofNullable(config).map(Config::getPublicEndpointEnabled).orElseGet(super::isPublicEndpointEnabled);
    }

    public void setPersistentDiskEnabled(Boolean enabled) {
        this.ensureConfig().setPersistentDiskEnabled(enabled);
    }

    public boolean isPersistentDiskEnabled() {
        final Boolean enabled = Optional.ofNullable(config).map(Config::getPersistentDiskEnabled).orElseGet(super::isPersistentDiskEnabled);
        return enabled && !this.getParent().isEnterpriseTier() && !this.getParent().isConsumptionTier();
    }

    public void setActiveDeploymentName(String name) {
        this.ensureConfig().setActiveDeploymentName(name);
    }

    @Nullable
    @Override
    public String getActiveDeploymentName() {
        return Optional.ofNullable(config).map(Config::getActiveDeploymentName).orElseGet(super::getActiveDeploymentName);
    }

    @Nullable
    @Override
    public SpringCloudDeployment getActiveDeployment() {
        return Optional.ofNullable(activeDeployment).orElseGet(super::getActiveDeployment);
    }

    @Nullable
    @Override
    public SpringCloudDeployment getCachedActiveDeployment() {
        return Optional.ofNullable(activeDeployment).orElseGet(super::getCachedActiveDeployment);
    }

    @Nonnull
    public SpringCloudDeploymentDraft updateOrCreateActiveDeployment() {
        final SpringCloudDeployment deployment = this.getActiveDeployment();
        final SpringCloudDeploymentDraft deploymentDraft;
        if (Objects.isNull(deployment)) {
            deploymentDraft = this.deployments().create(Optional.ofNullable(this.getActiveDeploymentName()).orElse("default"), null);
        } else if (!deployment.isDraft()) {
            deploymentDraft = (SpringCloudDeploymentDraft) deployment.update();
        } else {
            deploymentDraft = (SpringCloudDeploymentDraft) deployment;
        }
        this.setActiveDeployment(deploymentDraft);
        return deploymentDraft;
    }

    public void setActiveDeployment(SpringCloudDeployment activeDeployment) {
        this.activeDeployment = activeDeployment;
        Optional.ofNullable(activeDeployment).map(AbstractAzResource::getName).ifPresent(this::setActiveDeploymentName);
    }

    @Override
    public boolean isModified() {
        final String oldActiveDeploymentName = super.getActiveDeploymentName();
        final String newActiveDeploymentName = this.getActiveDeploymentName();
        final boolean newPublicEndpointEnabled = this.isPublicEndpointEnabled();
        final boolean newPersistentDiskEnabled = this.isPersistentDiskEnabled();

        return !Objects.equals(oldActiveDeploymentName, newActiveDeploymentName) && StringUtils.isNotBlank(newActiveDeploymentName) ||
            !Objects.equals(super.isPublicEndpointEnabled(), newPublicEndpointEnabled) ||
            !Objects.equals(super.isPersistentDiskEnabled(), newPersistentDiskEnabled);
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        private String name;
        @Nullable
        private String activeDeploymentName;
        @Nullable
        private Boolean publicEndpointEnabled;
        @Nullable
        private Boolean persistentDiskEnabled;
    }
}