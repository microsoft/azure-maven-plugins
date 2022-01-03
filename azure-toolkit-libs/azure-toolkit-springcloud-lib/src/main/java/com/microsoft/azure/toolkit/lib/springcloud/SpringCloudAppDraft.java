/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.resourcemanager.appplatform.implementation.SpringAppImpl;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
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

    SpringCloudAppDraft(@Nonnull String name, @Nonnull SpringCloudAppModule module) {
        super(name, module);
        this.setStatus(Status.DRAFT);
    }

    @Nullable
    private Config config;

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
        create.withDefaultActiveDeployment();
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
        if (Objects.isNull(oldActiveDeploymentName)) {
            if (StringUtils.equals(newActiveDeploymentName, DEFAULT_DEPLOYMENT_NAME) || StringUtils.isBlank(newActiveDeploymentName)) {
                modified = true;
                app.withDefaultActiveDeployment();
            }
        } else if (!Objects.equals(oldActiveDeploymentName, newActiveDeploymentName) && StringUtils.isNotBlank(newActiveDeploymentName)) {
            modified = true;
            app = app.withActiveDeployment(newActiveDeploymentName);
        }
        if (!Objects.equals(super.isPublicEndpointEnabled(), newPublicEndpointEnabled)) {
            modified = true;
            app = newPublicEndpointEnabled ? app.withDefaultPublicEndpoint() : app.withoutDefaultPublicEndpoint();
        }
        if (!Objects.equals(super.isPersistentDiskEnabled(), newPersistentDiskEnabled)) {
            modified = true;
            app = newPersistentDiskEnabled ? (app.parent().sku().tier().toLowerCase().startsWith("s") ? app.withPersistentDisk(STANDARD_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH) : app.withPersistentDisk(BASIC_TIER_DEFAULT_DISK_SIZE, DEFAULT_DISK_MOUNT_PATH)) : app.withPersistentDisk(0, null);
        }
        return modified;
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
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

    public void setActiveDeployment(SpringCloudDeployment activeDeployment) {
        this.activeDeployment = activeDeployment;
        Optional.ofNullable(activeDeployment).map(AbstractAzResource::getName).ifPresent(this::setActiveDeploymentName);
    }

    /**
     * {@code null} means not modified for properties
     */
    @Data
    private static class Config {
        @Nullable
        private String activeDeploymentName;
        @Nullable
        private Boolean publicEndpointEnabled;
        @Nullable
        private Boolean persistentDiskEnabled;
    }
}