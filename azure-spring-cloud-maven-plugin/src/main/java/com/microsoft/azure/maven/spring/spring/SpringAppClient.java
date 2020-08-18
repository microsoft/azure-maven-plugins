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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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

    public AppResourceInner createOrUpdateApp(SpringConfiguration configuration) {
        final AppResourceInner appResource = getApp();
        final AppResourceProperties appResourceProperties = appResource == null ? new AppResourceProperties()
                : appResource.properties();
        final PersistentDisk persistentDisk = isEnablePersistentStorage(configuration) ? getPersistentDiskOrDefault(appResourceProperties)
                : null;
        appResourceProperties.withPersistentDisk(persistentDisk);
        if (appResource == null) {
            final AppResourceInner tempAppResource = new AppResourceInner();
            tempAppResource.withProperties(appResourceProperties);
            return springManager.apps().inner().createOrUpdate(resourceGroup, clusterName, appName,
                    tempAppResource);
        } else {
            appResourceProperties.withPublicProperty(configuration.isPublic());
            return springManager.apps().inner().update(resourceGroup, clusterName, appName, appResource);
        }
    }

    public AppResourceInner activateDeployment(String deploymentName) {
        final AppResourceInner appResourceInner = getApp();
        final AppResourceProperties properties = appResourceInner.properties();

        if (!deploymentName.equals(properties.activeDeploymentName())) {
            properties.withActiveDeploymentName(deploymentName);
            return springManager.apps().inner().update(resourceGroup, clusterName, appName, appResourceInner);
        }
        return appResourceInner;
    }

    public DeploymentResourceInner getDeploymentByName(String deploymentName) {
        return springManager.deployments().inner().list(resourceGroup, clusterName, appName).stream()
                .filter(deploymentResourceInner -> deploymentResourceInner.name().equals(deploymentName)).findFirst()
                .orElse(null);
    }

    public String getActiveDeploymentName() {
        final AppResourceInner appResourceInner = getApp();
        return appResourceInner == null ? null : appResourceInner.properties().activeDeploymentName();
    }

    public SpringDeploymentClient getDeploymentClient(String deploymentName) {
        if (StringUtils.isEmpty(deploymentName)) {
            // When deployment name is not specified, get the active Deployment
            // Todo: throw exception when there are multi active deployments
            final String activeDeploymentName = getActiveDeploymentName();
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
        return deployments.stream().collect(Collectors.toList());
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

    private static boolean isEnablePersistentStorage(SpringConfiguration configuration) {
        return configuration != null && configuration.getDeployment() != null && configuration.getDeployment().isEnablePersistentStorage();
    }

    private static PersistentDisk getPersistentDiskOrDefault(AppResourceProperties appResourceProperties) {
        return appResourceProperties.persistentDisk() != null ? appResourceProperties.persistentDisk()
                : new PersistentDisk().withSizeInGB(DEFAULT_PERSISTENT_DISK_SIZE)
                        .withMountPath(DEFAULT_PERSISTENT_DISK_MOUNT_PATH);
    }
}
