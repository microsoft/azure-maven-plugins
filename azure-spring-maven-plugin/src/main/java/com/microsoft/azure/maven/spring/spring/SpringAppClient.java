/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.AppResourceProperties;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

public class SpringAppClient extends AbstractSpringClient {
    protected String appName;

    public static class Builder extends AbstractSpringClient.Builder<Builder> {
        protected String appName;

        public Builder withAppName(String appName) {
            this.appName = appName;
            return self();
        }

        public SpringAppClient build() {
            return new SpringAppClient(this);
        }

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
        final AppResourceProperties originProperties = appResource == null ? null : appResource.properties();
        final AppResourceProperties appResourceProperties = getAppResourceProperties(configuration, originProperties);
        return springManager.apps().inner()
                .createOrUpdate(resourceGroup, clusterName, appName, getAppResourceProperties(configuration, appResourceProperties));
    }

    public AppResourceInner updateActiveDeployment(String deploymentId) {
        final AppResourceInner appResourceInner = getApp();
        final AppResourceProperties properties = appResourceInner.properties();
        if (!properties.activeDeploymentId().equals(deploymentId)) {
            properties.withActiveDeploymentId(deploymentId);
            return springManager.apps().inner().createOrUpdate(resourceGroup, clusterName, appName, properties);
        }
        return appResourceInner;
    }

    public DeploymentResourceInner getDeploymentById(String deploymentId) {
        return springManager.deployments().inner().list(resourceGroup, clusterName, appName)
                .stream()
                .filter(deploymentResourceInner -> deploymentResourceInner.id().equals(deploymentId))
                .findFirst().orElse(null);
    }

    public String getActiveDeploymentId() {
        return getApp().properties().activeDeploymentId();
    }

    public SpringDeploymentClient getActiveDeploymentClient() {
        final String activeDeploymentId = getActiveDeploymentId();
        final DeploymentResourceInner activeDeployment = StringUtils.isEmpty(activeDeploymentId) ? null : getDeploymentById(activeDeploymentId);
        final String activeDeploymentName = activeDeployment == null ? String.format("deployment_%s", Utils.generateTimestamp()) : activeDeployment.name();
        return new SpringDeploymentClient(this, activeDeploymentName);
    }

    public void uploadArtifact(File artifact) throws MojoExecutionException {
        final String uploadUrl = springManager.apps().inner().getArtifactUploadUrl(resourceGroup, clusterName, appName).uploadUrl();
        Utils.uploadFileToStorage(artifact, uploadUrl);
    }

    public AppResourceInner getApp() {
        return springManager.apps().inner().get(resourceGroup, clusterName, appName);
    }

    private AppResourceProperties getAppResourceProperties(SpringConfiguration configuration, AppResourceProperties properties) {
        if (properties == null) {
            properties = new AppResourceProperties();
        }
        properties.withPort(configuration.getPort())
                .withPublicProperty(configuration.isPublic());
        return properties;
    }
}
