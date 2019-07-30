/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.ArtifactResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.maven.spring.spring.SpringAppClient;
import com.microsoft.azure.maven.spring.spring.SpringDeploymentClient;
import com.microsoft.azure.maven.spring.spring.SpringServiceUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractSpringMojo {

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        final SpringConfiguration configuration = this.getConfiguration();
        final SpringAppClient springAppClient = SpringServiceUtils.newSpringAppClient(configuration);
        // Create or update new App
        AppResourceInner app = springAppClient.createOrUpdateApp(configuration);
        // Upload artifact
        final File toDeploy = Utils.getArtifactFromConfiguration(configuration);
        springAppClient.uploadArtifact(toDeploy);
        // Create or update deployment
        final SpringDeploymentClient deploymentClient = springAppClient.getActiveDeploymentClient();
        final ArtifactResourceInner artifact = deploymentClient.createArtifact(configuration, toDeploy); // Create artifact first
        final DeploymentResourceInner deployment = deploymentClient.createOrUpdateDeployment(configuration.getDeployment(), artifact);
        // Update the app with new deployment
        app = springAppClient.updateActiveDeployment(deployment.id());
        // Update deployment, show url
        getLog().info(app.properties().url());
    }

    @Override
    protected void updateTelemetry(SpringConfiguration springConfiguration) {
        super.updateTelemetry(springConfiguration);
        // Todo update deploy mojo telemetries with real value
        telemetries.put("authMethod", "Pom Configuration");
        telemetries.put("isServicePrincipal", "false");
        telemetries.put("isKeyEncrypted", "");
        telemetries.put("isCreateNewApp", "");
        telemetries.put("isUpdateConfiguration", "");
    }
}
