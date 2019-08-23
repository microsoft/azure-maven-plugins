/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.AppResourceProperties;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.maven.spring.SpringConfiguration;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class SpringAppClient extends AbstractSpringClient {

    public static final String DEFAULT_DEPLOYMENT_NAME = "init";

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

    public AppResourceInner activateDeployment(String deploymentName) {
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
        final ResourceUploadDefinitionInner resourceUploadDefinition = springManager.apps().inner().getResourceUploadUrl(resourceGroup, clusterName, appName);
        Utils.uploadFileToStorage(artifact, resourceUploadDefinition.uploadUrl());
        return resourceUploadDefinition;
    }

    public List<DeploymentResourceInner> getDeployments() {
        final PagedList<DeploymentResourceInner> deployments = springManager.deployments().inner().list(resourceGroup, clusterName, appName);
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
}
