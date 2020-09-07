/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.appplatform.v2020_07_01.AppResourceProperties;
import com.microsoft.azure.management.appplatform.v2020_07_01.PersistentDisk;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpringAppClient extends AbstractSpringClient {

    protected static final int DEFAULT_PERSISTENT_DISK_SIZE = 50;
    protected static final String DEFAULT_DEPLOYMENT_NAME = "default";
    protected static final String DEFAULT_PERSISTENT_DISK_MOUNT_PATH = "/persistent";

    protected String appName;

    public static class Builder extends AbstractSpringClient.Builder<Builder> {
        protected String appName;

        public Builder withAppName(String appName) {
            this.appName = appName;
            return self();
        }

        @Override
        public SpringAppClient build() {
            return new SpringAppClient(this);
        }

        @Override
        public Builder self() {
            return this;
        }
    }

    public SpringAppClient(Builder builder) {
        super(builder);
        this.appName = builder.appName;
    }

    @Nonnull
    public AppResourceInner createOrUpdateApp(final AppResourceInner app, final SpringConfiguration configuration) {
        return Objects.isNull(app) ? createApp(configuration) : updateApp(app, configuration);
    }

    @Nonnull
    public AppResourceInner createApp(final SpringConfiguration configuration) {
        final AppResourceProperties properties = mergeConfigurationIntoProperties(configuration, new AppResourceProperties());
        return this.createApp(properties);
    }

    @Nonnull
    public AppResourceInner createApp(final AppResourceProperties properties) {
        final AppResourceInner app = new AppResourceInner().withProperties(properties);
        return springManager.apps().inner().createOrUpdate(resourceGroup, clusterName, appName, app);
    }

    @Nonnull
    public AppResourceInner updateApp(final AppResourceInner app, final SpringConfiguration configuration) {
        final AppResourceProperties properties = mergeConfigurationIntoProperties(configuration, app.properties());
        return this.updateApp(app, properties);
    }

    @Nonnull
    public AppResourceInner updateApp(final AppResourceInner app, final AppResourceProperties properties) {
        app.withProperties(properties);
        return springManager.apps().inner().update(resourceGroup, clusterName, appName, app);
    }

    public DeploymentResourceInner getDeploymentByName(String deploymentName) {
        return springManager.deployments().inner().list(resourceGroup, clusterName, appName).stream()
                .filter(deploymentResourceInner -> deploymentResourceInner.name().equals(deploymentName)).findFirst()
                .orElse(null);
    }

    public static String getActiveDeploymentName(final AppResourceInner appResourceInner) {
        return appResourceInner == null ? null : appResourceInner.properties().activeDeploymentName();
    }

    public SpringDeploymentClient getDeploymentClient(String deploymentName, final AppResourceInner app) {
        if (StringUtils.isEmpty(deploymentName)) {
            // When deployment name is not specified, get the active Deployment
            // Todo: throw exception when there are multi active deployments
            final String activeDeploymentName = getActiveDeploymentName(app);
            deploymentName = StringUtils.isEmpty(activeDeploymentName) ? DEFAULT_DEPLOYMENT_NAME : activeDeploymentName;
        }
        return new SpringDeploymentClient(this, deploymentName);
    }

    public ResourceUploadDefinitionInner uploadArtifact(File artifact) throws MojoExecutionException {
        final ResourceUploadDefinitionInner resourceUploadDefinition = springManager.apps().inner()
                .getResourceUploadUrl(resourceGroup, clusterName, appName);
        Utils.uploadFileToStorage(artifact, resourceUploadDefinition.uploadUrl());
        return resourceUploadDefinition;
    }

    public List<DeploymentResourceInner> getDeployments() {
        final PagedList<DeploymentResourceInner> deployments = springManager.deployments().inner().list(resourceGroup,
                clusterName, appName);
        deployments.loadAll();
        return new ArrayList<>(deployments);
    }

    public String getApplicationUrl() {
        return getApp().properties().url();
    }

    public boolean isPublic() {
        return getApp().properties().publicProperty();
    }

    public AppResourceInner getApp() {
        return springManager.apps().inner().get(resourceGroup, clusterName, appName, "true");
    }

    public String getAppName() {
        return appName;
    }

    public static AppResourceProperties mergeConfigurationIntoProperties(SpringConfiguration configuration, AppResourceProperties properties) {
        final PersistentDisk persistentDisk = isEnablePersistentStorage(configuration) ? getPersistentDiskOrDefault(properties) : null;
        if (StringUtils.isNotEmpty(configuration.getActiveDeploymentName())) {
            properties.withActiveDeploymentName(configuration.getActiveDeploymentName());
        }
        return properties.withPersistentDisk(persistentDisk).withPublicProperty(configuration.isPublic());
    }

    private static boolean isEnablePersistentStorage(SpringConfiguration configuration) {
        return configuration != null && configuration.getDeployment() != null && configuration.getDeployment().isEnablePersistentStorage();
    }

    private static PersistentDisk getPersistentDiskOrDefault(AppResourceProperties appResourceProperties) {
        final PersistentDisk disk = appResourceProperties.persistentDisk();
        return disk == null || disk.sizeInGB() <= 0 ? new PersistentDisk().withSizeInGB(DEFAULT_PERSISTENT_DISK_SIZE)
                .withMountPath(DEFAULT_PERSISTENT_DISK_MOUNT_PATH) : disk;
    }
}
