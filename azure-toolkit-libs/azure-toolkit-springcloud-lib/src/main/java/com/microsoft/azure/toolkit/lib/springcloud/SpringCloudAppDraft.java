/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppImpl;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.UserSourceType;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class SpringCloudAppDraft extends SpringCloudApp implements AzResource.Draft<SpringCloudApp, SpringApp> {
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    public static final String DEFAULT_DISK_MOUNT_PATH = "/persistent";
    public static final String DEFAULT_DEPLOYMENT_NAME = "default";
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Cloud</a>
     */
    public static final int BASIC_TIER_DEFAULT_DISK_SIZE = 1;
    /**
     * @see <a href="https://azure.microsoft.com/en-us/pricing/details/spring-cloud/">Pricing - Azure Spring Cloud</a>
     */
    public static final int STANDARD_TIER_DEFAULT_DISK_SIZE = 50;
    private SpringCloudDeployment activeDeployment;
    @Nullable
    private Config config;

    SpringCloudAppDraft(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.setStatus(Status.DRAFT);
    }

    public void setConfig(SpringCloudAppConfig c) {
        this.setName(c.getAppName());
        this.setActiveDeploymentName(c.getActiveDeploymentName());
        this.setPublicEndpointEnabled(c.getIsPublic());
        final SpringCloudDeploymentConfig deploymentConfig = c.getDeployment();
        final SpringCloudDeploymentDraft deploymentDraft = this.updateOrCreateActiveDeployment();
        this.setPersistentDiskEnabled(deploymentConfig.getEnablePersistentStorage());
        deploymentDraft.setConfig(deploymentConfig);
    }

    public SpringCloudAppConfig getConfig() {
        final SpringCloudDeploymentConfig deploymentConfig = activeDeployment instanceof SpringCloudDeploymentDraft ?
            ((SpringCloudDeploymentDraft) activeDeployment).getConfig() : SpringCloudDeploymentConfig.builder().build();
        deploymentConfig.setEnablePersistentStorage(this.isPersistentDiskEnabled());
        return SpringCloudAppConfig.builder()
            .subscriptionId(this.getSubscriptionId())
            .clusterName(this.getParent().getName())
            .appName(this.getName())
            .resourceGroup(this.getResourceGroupName())
            .isPublic(this.isPublicEndpointEnabled())
            .activeDeploymentName(this.getActiveDeploymentName())
            .deployment(deploymentConfig)
            .build();
    }

    @Override
    public void reset() {
        this.config = null;
        this.activeDeployment = null;
    }

    @Override
    public SpringApp createResourceInAzure() {
        final String appName = this.getName();
        final SpringService service = Objects.requireNonNull(this.getParent().getRemote());
        final SpringAppImpl create = (SpringAppImpl) service.apps().define(appName);
        modify(create);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating app({0})...", appName));
        final SpringApp app = create.create();
        messager.success(AzureString.format("App({0}) is successfully created.", appName));
        return app;
    }

    @Override
    public SpringApp updateResourceInAzure(@Nonnull SpringApp origin) {
        final SpringAppImpl update = ((SpringAppImpl) origin.update());
        if (modify(update)) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating app({0})...", origin.name()));
            origin = update.apply();
            messager.success(AzureString.format("App({0}) is successfully updated.", origin.name()));
            messager.warning(UPDATE_APP_WARNING);
        }
        return origin;
    }

    boolean modify(@Nonnull SpringAppImpl app) {
        boolean modified = false;
        final String oldActiveDeploymentName = super.getActiveDeploymentName();
        final String newActiveDeploymentName = this.getActiveDeploymentName();
        final boolean newPublicEndpointEnabled = this.isPublicEndpointEnabled();
        final boolean newPersistentDiskEnabled = this.isPersistentDiskEnabled();

        // refer https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/resourcemanager/azure-resourcemanager-samples/src/main/java/com/azure/
        // resourcemanager/appplatform/samples/ManageSpringCloud.java#L122-L129
        if (!Objects.equals(oldActiveDeploymentName, newActiveDeploymentName) && StringUtils.isNotBlank(newActiveDeploymentName)) {
            modified = true;
            app = (SpringAppImpl) app.defineActiveDeployment(newActiveDeploymentName).withExistingSource(UserSourceType.JAR, "<default>").attach();
        }
        if (!Objects.equals(super.isPublicEndpointEnabled(), newPublicEndpointEnabled)) {
            modified = true;
            app = newPublicEndpointEnabled ? app.withDefaultPublicEndpoint() : app.withoutDefaultPublicEndpoint();
        }
        if (!Objects.equals(super.isPersistentDiskEnabled(), newPersistentDiskEnabled)) {
            modified = true;
            app = newPersistentDiskEnabled ? (app.parent().sku().tier().toLowerCase().startsWith("s") ?
                app.withPersistentDisk(STANDARD_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH) :
                app.withPersistentDisk(BASIC_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH)) :
                app.withPersistentDisk(0, null);
        }
        return modified;
    }

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
        return Optional.ofNullable(config).map(Config::getPersistentDiskEnabled).orElseGet(super::isPersistentDiskEnabled);
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

    @Nonnull
    public SpringCloudDeploymentDraft updateOrCreateActiveDeployment() {
        final String activeDeploymentName = Optional.ofNullable(this.getActiveDeploymentName()).orElse("default");
        final SpringCloudDeploymentDraft deploymentDraft = (SpringCloudDeploymentDraft) Optional
            .ofNullable(super.getActiveDeployment()).map(AbstractAzResource::update)
            .orElseGet(() -> this.deployments().updateOrCreate(activeDeploymentName, this.getResourceGroupName()));
        this.setActiveDeployment(deploymentDraft);
        return deploymentDraft;
    }

    public void setActiveDeployment(SpringCloudDeployment activeDeployment) {
        this.activeDeployment = activeDeployment;
        Optional.ofNullable(activeDeployment).map(AbstractAzResource::getName).ifPresent(this::setActiveDeploymentName);
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