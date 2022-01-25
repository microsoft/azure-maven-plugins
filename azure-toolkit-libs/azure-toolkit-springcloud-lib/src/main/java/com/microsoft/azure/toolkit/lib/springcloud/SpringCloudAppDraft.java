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
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.Data;
import lombok.Getter;
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
    @Getter
    @Nullable
    private final SpringCloudApp origin;
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
        if (this.activeDeployment instanceof Draft) {
            ((Draft<?, ?>) this.activeDeployment).reset();
        }
        this.activeDeployment = null;
    }

    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringApp createResourceInAzure() {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
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
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SpringApp updateResourceInAzure(@Nonnull SpringApp origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
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

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getName()) || Objects.equals(this.config.getName(), super.getName()) ||
            Objects.isNull(this.config.getActiveDeploymentName()) || Objects.equals(this.config.getActiveDeploymentName(), super.getActiveDeploymentName()) ||
            Objects.equals(this.isPublicEndpointEnabled(), super.isPublicEndpointEnabled()) ||
            Objects.equals(this.isPersistentDiskEnabled(), super.isPersistentDiskEnabled()) ||
            !(this.activeDeployment instanceof Draft) || !((SpringCloudDeploymentDraft) this.activeDeployment).isModified();
        return !notModified;
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