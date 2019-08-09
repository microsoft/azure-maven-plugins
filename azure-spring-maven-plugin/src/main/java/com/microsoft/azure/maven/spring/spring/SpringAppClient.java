/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.AppResourceProperties;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.ResourceUploadDefinitionInner;
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
        final AppResourceProperties appResourceProperties = appResource == null ?
                new AppResourceProperties() : appResource.properties();
        appResourceProperties.withPublicProperty(configuration.isPublic());
        if (appResource == null) {
            return springManager.apps().inner()
                    .createOrUpdate(resourceGroup, clusterName, appName, appResourceProperties);
        } else {
            return springManager.apps().inner().update(resourceGroup, clusterName, appName, appResourceProperties);
        }
    }

    public AppResourceInner updateActiveDeployment(String deploymentName) {
        final AppResourceInner appResourceInner = getApp();
        final AppResourceProperties properties = appResourceInner.properties();

        if (!deploymentName.equals(properties.activeDeploymentName())) {
            properties.withActiveDeploymentName(deploymentName);
            return springManager.apps().inner().update(resourceGroup, clusterName, appName, properties);
        }
        return appResourceInner;
    }

    public DeploymentResourceInner getDeploymentByName(String deploymentName) {
        return springManager.deployments().inner().list(resourceGroup, clusterName, appName)
                .stream()
                .filter(deploymentResourceInner -> deploymentResourceInner.name().equals(deploymentName))
                .findFirst().orElse(null);
    }

    public String getActiveDeploymentName() {
        return getApp().properties().activeDeploymentName();
    }

    public SpringDeploymentClient getActiveDeploymentClient() {
        final String activeDeploymentId = getActiveDeploymentName();
        final DeploymentResourceInner activeDeployment = StringUtils.isEmpty(activeDeploymentId) ? null : getDeploymentByName(activeDeploymentId);
        final String activeDeploymentName = activeDeployment == null ? String.format("deployment-%s", Utils.generateTimestamp()) : activeDeployment.name();
        return new SpringDeploymentClient(this, activeDeploymentName);
    }

    public ResourceUploadDefinitionInner uploadArtifact(File artifact) throws MojoExecutionException {
        final ResourceUploadDefinitionInner resourceUploadDefinition = springManager.apps().inner().getResourceUploadUrl(resourceGroup, clusterName, appName);
        Utils.uploadFileToStorage(artifact, resourceUploadDefinition.uploadUrl());
        return resourceUploadDefinition;
    }

    public AppResourceInner getApp() {
        return springManager.apps().inner().get(resourceGroup, clusterName, appName);
    }
}
