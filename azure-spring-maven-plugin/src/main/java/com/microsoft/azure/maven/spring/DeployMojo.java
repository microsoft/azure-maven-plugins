/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.DeploymentInstance;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.DeploymentResourceStatus;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.prompt.DefaultPrompter;
import com.microsoft.azure.maven.spring.prompt.IPrompter;
import com.microsoft.azure.maven.spring.spring.SpringAppClient;
import com.microsoft.azure.maven.spring.spring.SpringDeploymentClient;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_NEW_APP;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_UPDATE_CONFIGURATION;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractSpringMojo {

    @Parameter(property = "createInactive")
    protected boolean createInactive;

    @Parameter(property = "skipConfirm")
    private boolean skipConfirm;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}", readonly = true)
    private File defaultArtifact;

    protected static final int GET_STATUS_TIMEOUT = 30;
    protected static final String PROJECT_SKIP = "Skip pom project";
    protected static final String PROJECT_NOT_SUPPORT = "`azure-spring:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
    protected static final String DEPLOYMENT_NOT_EXIST = "Deployment %s doesn't exist in app %s, please check the " +
            "configuration or use -DcreateInactive to create a inactive one";
    protected static final String ARTIFACT_NOT_SUPPORTED = "Target file doesn't exist or is not executable, please " +
            "check the configuration.";
    protected static final String GET_DEPLOYMENT_STATUS_FAIL = "Fail to get deployment status in %d s";
    protected static final String GET_APP_URL_SUCCESSFULLY = "Application url : %s";
    protected static final String GET_APP_URL_FAIL = "Fail to get application url in %d s";
    protected static final String STATUS_CREATE_OR_UPDATE_APP = "Creating/Updating app...";
    protected static final String STATUS_CREATE_OR_UPDATE_DEPLOYMENT = "Creating/Updating deployment...";
    protected static final String STATUS_UPLOADING_ARTIFACTS = "Uploading artifacts...";
    protected static final String CONFIRM_PROMPT_START = "`azure-spring:deploy` will do the following jobs";
    protected static final String CONFIRM_PROMPT_CREATE_NEW_APP = "Create new app %s";
    protected static final String CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT = "Create new deployment %s in app %s";
    protected static final String CONFIRM_PROMPT_UPDATE_DEPLOYMENT = "Update deployment %s in app %s";
    protected static final String CONFIRM_PROMPT_CONFIRM = "Do you confirm the behaviors?";

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!checkProjectPackaging(project)) {
            return;
        }
        // Init spring clients, and prompt users to confirm
        final SpringConfiguration configuration = this.getConfiguration();
        final Deployment deploymentConfiguration = configuration.getDeployment();
        final SpringAppClient springAppClient = getSpringServiceClient().newSpringAppClient(configuration);
        final SpringDeploymentClient deploymentClient = springAppClient.
                getDeploymentClient(deploymentConfiguration.getDeploymentName());
        if (!shouldSkipConfirm() && !confirmDeploy(springAppClient, deploymentClient)) {
            getLog().info("Terminate deployment");
            return;
        }
        // Prepare telemetries
        traceTelemetry(springAppClient, configuration);
        // Create or update new App
        getLog().info(STATUS_CREATE_OR_UPDATE_APP);
        springAppClient.createOrUpdateApp(configuration);
        // Upload artifact
        getLog().info(STATUS_UPLOADING_ARTIFACTS);
        final File toDeploy = isResourceSpecified(configuration) ? Utils.getArtifactFromConfiguration(configuration) : defaultArtifact;
        if (toDeploy == null || !Utils.isExecutableJar(toDeploy)) {
            throw new MojoExecutionException(ARTIFACT_NOT_SUPPORTED);
        }
        final ResourceUploadDefinitionInner uploadDefinition = springAppClient.uploadArtifact(toDeploy);
        // Create or update deployment
        getLog().info(STATUS_CREATE_OR_UPDATE_DEPLOYMENT);
        if (deploymentClient.isDeploymentExist() || createInactive) {
            // Update existing deployment or create an inactive one
            deploymentClient.createOrUpdateDeployment(deploymentConfiguration, uploadDefinition);
        } else if (springAppClient.getDeployments().size() == 0) {
            // Create new deployment and active it
            final DeploymentResourceInner deployment = deploymentClient.createOrUpdateDeployment(deploymentConfiguration, uploadDefinition);
            springAppClient.updateActiveDeployment(deployment.name());
        } else {
            throw new IllegalArgumentException(DEPLOYMENT_NOT_EXIST);
        }

        // Showing deployment status and public url
        getDeploymentStatus(deploymentClient);
        getPublicUrl(springAppClient);
    }

    protected void getPublicUrl(SpringAppClient springAppClient) {
        if (!springAppClient.isPublic()) {
            return;
        }
        final String publicUrl = Utils.executeCallableWithPrompt(() -> {
            String url = springAppClient.getApplicationUrl();
            while (StringUtils.isEmpty(url) || StringUtils.equalsIgnoreCase(url, "pending")) {
                url = springAppClient.getApplicationUrl();
            }
            return url;
        }, String.format("Getting the public url", springAppClient.getApp()), GET_STATUS_TIMEOUT);
        if (publicUrl == null) {
            getLog().warn(String.format(GET_APP_URL_FAIL, GET_STATUS_TIMEOUT));
        } else {
            System.out.println(String.format(GET_APP_URL_SUCCESSFULLY, publicUrl));
        }
    }

    protected void getDeploymentStatus(SpringDeploymentClient springDeploymentClient) {
        final DeploymentResourceInner deploymentResource = Utils.executeCallableWithPrompt(() -> {
            DeploymentResourceInner deployment = springDeploymentClient.getDeployment();
            while (!isDeploymentDone(deployment)) {
                Thread.sleep(2000);
                deployment = springDeploymentClient.getDeployment();
            }
            return deployment;
        }, "Getting deployment status", GET_STATUS_TIMEOUT);
        if (deploymentResource == null) {
            getLog().warn(String.format(GET_DEPLOYMENT_STATUS_FAIL, GET_STATUS_TIMEOUT));
        } else {
            showDeploymentStatus(deploymentResource);
        }
    }

    protected void showDeploymentStatus(DeploymentResourceInner deploymentResource) {
        System.out.println(String.format("Deployment Status: %s", deploymentResource.properties().status()));
        for (final DeploymentInstance instance : deploymentResource.properties().instances()) {
            System.out.println(String.format("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s", instance.name(), instance.status()
                    , instance.reason(), instance.discoveryStatus()));
        }
    }

    protected boolean isDeploymentDone(DeploymentResourceInner deploymentResource) {
        if (deploymentResource.properties().status() == DeploymentResourceStatus.PROCESSING) {
            return false;
        }
        return !deploymentResource.properties().instances().stream()
                .anyMatch(instance -> instance.status().equalsIgnoreCase("pending"));
    }

    protected boolean shouldSkipConfirm() {
        // Skip confirm when -DskipConfirm or in batch model
        return skipConfirm || (this.settings != null && !this.settings.isInteractiveMode());
    }

    protected boolean confirmDeploy(SpringAppClient springAppClient, SpringDeploymentClient deploymentClient) throws MojoFailureException {
        final IPrompter prompter = new DefaultPrompter();
        final List<String> operations = getOperations(springAppClient, deploymentClient);
        try {
            System.out.println(CONFIRM_PROMPT_START);
            for (int i = 1; i <= operations.size(); i++) {
                System.out.println(String.format("%-2d  %s", i, operations.get(i - 1)));
            }
            return prompter.promoteYesNo(true, CONFIRM_PROMPT_CONFIRM, true);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        } finally {
            try {
                prompter.close();
            } catch (IOException e) {
                //swallow final exception
            }
        }
    }

    protected List<String> getOperations(SpringAppClient springAppClient, SpringDeploymentClient deploymentClient) {
        final List<String> operations = new ArrayList<>();
        final boolean isCreateNewApp = springAppClient.getApp() == null;
        final boolean isCreateNewDeployment = deploymentClient.getDeployment() == null;
        if (isCreateNewApp) {
            operations.add(String.format(CONFIRM_PROMPT_CREATE_NEW_APP, springAppClient.getAppName()));
        }
        if (isCreateNewDeployment && !createInactive && springAppClient.getDeployments().size() > 0) {
            throw new IllegalArgumentException(DEPLOYMENT_NOT_EXIST);
        }
        final String deploymentPrompt = isCreateNewDeployment ?
                String.format(CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT, deploymentClient.getDeploymentName(), deploymentClient.getAppName()) :
                String.format(CONFIRM_PROMPT_UPDATE_DEPLOYMENT, deploymentClient.getDeploymentName(), deploymentClient.getAppName());
        operations.add(deploymentPrompt);
        return operations;
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

    protected boolean isResourceSpecified(SpringConfiguration springConfiguration) {
        final Deployment deploymentConfiguration = springConfiguration.getDeployment();
        return deploymentConfiguration.getResources() != null && deploymentConfiguration.getResources().size() > 0;
    }

    protected void traceTelemetry(SpringAppClient springAppClient, SpringConfiguration springConfiguration) {
        traceAuth();
        traceConfiguration(springConfiguration);
        traceDeployment(springAppClient, springConfiguration);
    }

    protected void traceDeployment(SpringAppClient springAppClient, SpringConfiguration springConfiguration) {
        final boolean isNewApp = springAppClient.getApp() == null;
        final boolean isUpdateConfiguration = false;
        telemetries.put(TELEMETRY_KEY_IS_CREATE_NEW_APP, String.valueOf(isNewApp));
        telemetries.put(TELEMETRY_KEY_IS_UPDATE_CONFIGURATION, String.valueOf(isUpdateConfiguration));
    }
}
