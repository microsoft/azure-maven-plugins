/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.maven.spring.spring.SpringAppClient;
import com.microsoft.azure.maven.spring.spring.SpringDeploymentClient;
import com.microsoft.azure.maven.spring.spring.SpringServiceUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_NEW_APP;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_UPDATE_CONFIGURATION;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractSpringMojo {

    protected static final String PROJECT_NOT_SUPPORT = "`azure-spring:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
    protected static final String PROJECT_SKIP = "Skip pom project";

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!checkProjectPackaging(project)) {
            return;
        }
        final SpringConfiguration configuration = this.getConfiguration();
        final SpringAppClient springAppClient = SpringServiceUtils.newSpringAppClient(configuration);
        // Prepare telemetries
        traceTelemetry(springAppClient, configuration);
        // Create or update new App
        AppResourceInner app = springAppClient.createOrUpdateApp(configuration);
        // Upload artifact
        final File toDeploy = Utils.getArtifactFromConfiguration(configuration);
        final ResourceUploadDefinitionInner uploadDefinition = springAppClient.uploadArtifact(toDeploy);
        // Create or update deployment
        final SpringDeploymentClient deploymentClient = springAppClient.getActiveDeploymentClient();
        final DeploymentResourceInner deployment = deploymentClient.createOrUpdateDeployment(configuration.getDeployment(), uploadDefinition);
        // Update the app with new deployment
        app = springAppClient.updateActiveDeployment(deployment.name());
        // Update deployment, show url
        getLog().info(app.properties().url());
        getLog().info(deploymentClient.getDeploymentStatus().toString());
    }

    protected boolean checkProjectPackaging(MavenProject project) throws MojoExecutionException {
        if (Utils.isJarPackagingProject(project)) {
            return true;
        } else if (Utils.isPomPackagingProject(project)) {
            getLog().info(PROJECT_SKIP);
            return false;
        } else {
            throw new MojoExecutionException(String.format(PROJECT_NOT_SUPPORT, project.getPackaging()));
        }
    }

    protected void traceTelemetry(SpringAppClient springAppClient, SpringConfiguration springConfiguration) {
        traceAuth();
        traceConfiguration(springConfiguration);
        traceDeployment(springAppClient, springConfiguration);
    }

    protected void traceDeployment(SpringAppClient springAppClient, SpringConfiguration springConfiguration) {
        final boolean isNewApp = springAppClient.getApp() == null;
        final boolean isUpdateConfiguration = springConfiguration.getDeployment().getResources().isEmpty();
        telemetries.put(TELEMETRY_KEY_IS_CREATE_NEW_APP, String.valueOf(isNewApp));
        telemetries.put(TELEMETRY_KEY_IS_UPDATE_CONFIGURATION, String.valueOf(isUpdateConfiguration));
    }
}
