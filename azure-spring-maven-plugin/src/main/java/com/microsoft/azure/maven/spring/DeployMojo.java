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
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.azure.maven.spring.prompt.DefaultPrompter;
import com.microsoft.azure.maven.spring.prompt.IPrompter;
import com.microsoft.azure.maven.spring.spring.SpringAppClient;
import com.microsoft.azure.maven.spring.spring.SpringDeploymentClient;
import com.microsoft.azure.maven.spring.utils.MavenUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_DEPLOYMENT;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_NEW_APP;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractSpringMojo {

    @Parameter(property = "noWait")
    private boolean noWait;

    @Parameter(property = "prompt")
    private boolean prompt;

    protected static final int GET_STATUS_TIMEOUT = 30;
    protected static final int GET_URL_TIMEOUT = 60;
    protected static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    protected static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    protected static final String PROJECT_NOT_SUPPORT = "`azure-spring:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
    protected static final String GET_APP_URL_SUCCESSFULLY = "Application url : %s";
    protected static final String GET_APP_URL_FAIL = "Fail to get application url";
    protected static final String GET_APP_URL_FAIL_WITH_TIMEOUT = "Fail to get application url in %d s";
    protected static final String STATUS_CREATE_OR_UPDATE_APP = "Creating/Updating the app...";
    protected static final String STATUS_CREATE_OR_UPDATE_APP_DONE = "Successfully created/updated the app.";
    protected static final String STATUS_CREATE_OR_UPDATE_DEPLOYMENT = "Creating/Updating the deployment...";
    protected static final String STATUS_CREATE_OR_UPDATE_DEPLOYMENT_DONE = "Successfully created/updated the deployment.";
    protected static final String STATUS_UPLOADING_ARTIFACTS = "Uploading artifacts...";
    protected static final String STATUS_UPLOADING_ARTIFACTS_DONE = "Successfully uploaded the artifacts.";
    protected static final String CONFIRM_PROMPT_START = "`azure-spring:deploy` will perform the following tasks";
    protected static final String CONFIRM_PROMPT_CREATE_NEW_APP = "Create new app [%s]";
    protected static final String CONFIRM_PROMPT_UPDATE_APP = "Update app [%s]";
    protected static final String CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT = "Create new deployment [%s] in app [%s]";
    protected static final String CONFIRM_PROMPT_UPDATE_DEPLOYMENT = "Update deployment [%s] in app [%s]";
    protected static final String CONFIRM_PROMPT_ACTIVATE_DEPLOYMENT = "Set [%s] as the active deployment of app [%s]";
    protected static final String CONFIRM_PROMPT_CONFIRM = "Perform the above tasks?";

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!checkProjectPackaging(project) || !checkConfiguration()) {
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
        traceTelemetry(springAppClient, deploymentClient, configuration);
        // Create or update new App
        getLog().info(STATUS_CREATE_OR_UPDATE_APP);
        springAppClient.createOrUpdateApp(configuration);
        getLog().info(STATUS_CREATE_OR_UPDATE_APP_DONE);
        // Upload artifact
        getLog().info(STATUS_UPLOADING_ARTIFACTS);
        final File toDeploy = isResourceSpecified(configuration) ? Utils.getArtifactFromConfiguration(configuration) :
                Utils.getArtifactFromTargetFolder(project);
        final ResourceUploadDefinitionInner uploadDefinition = springAppClient.uploadArtifact(toDeploy);
        getLog().info(STATUS_UPLOADING_ARTIFACTS_DONE);
        // Create or update deployment
        final boolean createNewDeployment = deploymentClient.getDeployment() == null;
        getLog().info(STATUS_CREATE_OR_UPDATE_DEPLOYMENT);
        deploymentClient.createOrUpdateDeployment(deploymentConfiguration, uploadDefinition);
        if (StringUtils.isEmpty(springAppClient.getActiveDeploymentName()) && createNewDeployment) {
            // Active deployment if no existing active deployment
            springAppClient.activateDeployment(deploymentClient.getDeploymentName());
        }
        getLog().info(STATUS_CREATE_OR_UPDATE_DEPLOYMENT_DONE);
        // Showing deployment status and public url
        getDeploymentStatus(deploymentClient);
        getPublicUrl(springAppClient);
    }

    protected void getPublicUrl(SpringAppClient springAppClient) {
        if (!springAppClient.isPublic()) {
            return;
        }
        String publicUrl = springAppClient.getApplicationUrl();
        if (!noWait) {
            publicUrl = Utils.executeCallableWithPrompt(() -> {
                String url = springAppClient.getApplicationUrl();
                while (StringUtils.isEmpty(url) || StringUtils.equalsIgnoreCase(url, "pending")) {
                    url = springAppClient.getApplicationUrl();
                }
                return url;
            }, String.format("Getting the public url", springAppClient.getApp()), GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            final String message = noWait ? GET_APP_URL_FAIL :
                    String.format(GET_APP_URL_FAIL_WITH_TIMEOUT, GET_URL_TIMEOUT);
            getLog().warn(message);
        } else {
            System.out.println(String.format(GET_APP_URL_SUCCESSFULLY, publicUrl));
        }
    }

    protected void getDeploymentStatus(SpringDeploymentClient springDeploymentClient) {
        DeploymentResourceInner deploymentResource = null;
        if (!noWait) {
            deploymentResource = Utils.executeCallableWithPrompt(() -> {
                DeploymentResourceInner deployment = springDeploymentClient.getDeployment();
                while (!isDeploymentDone(deployment)) {
                    Thread.sleep(2000);
                    deployment = springDeploymentClient.getDeployment();
                }
                return deployment;
            }, "Getting deployment status", GET_STATUS_TIMEOUT);
        }
        if (deploymentResource == null) {
            deploymentResource = springDeploymentClient.getDeployment();
        }
        showDeploymentStatus(deploymentResource);
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
                .anyMatch(instance -> instance.status().equalsIgnoreCase("waiting"));
    }

    protected boolean shouldSkipConfirm() {
        // Skip confirm when -Dprompt or in batch model
        return !prompt || (this.settings != null && !this.settings.isInteractiveMode());
    }

    protected boolean confirmDeploy(SpringAppClient springAppClient, SpringDeploymentClient deploymentClient) throws MojoFailureException {
        final IPrompter prompter = new DefaultPrompter();
        final List<String> operations = getOperations(springAppClient, deploymentClient);
        try {
            System.out.println(CONFIRM_PROMPT_START);
            for (int i = 1; i <= operations.size(); i++) {
                System.out.println(String.format("%-2d  %s", i, operations.get(i - 1)));
            }
            return prompter.promoteYesNo(CONFIRM_PROMPT_CONFIRM, true, true);
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
        final String appPrompt = isCreateNewApp ?
                String.format(CONFIRM_PROMPT_CREATE_NEW_APP, TextUtils.blue(deploymentClient.getAppName())) :
                String.format(CONFIRM_PROMPT_UPDATE_APP, TextUtils.blue(deploymentClient.getAppName()));
        operations.add(appPrompt);

        final boolean isCreateNewDeployment = deploymentClient.getDeployment() == null;
        final String deploymentPrompt = isCreateNewDeployment ?
                String.format(CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT, TextUtils.blue(deploymentClient.getDeploymentName()),
                        TextUtils.blue(deploymentClient.getAppName())) :
                String.format(CONFIRM_PROMPT_UPDATE_DEPLOYMENT, TextUtils.blue(deploymentClient.getDeploymentName()),
                        TextUtils.blue(deploymentClient.getAppName()));
        operations.add(deploymentPrompt);

        if (StringUtils.isEmpty(springAppClient.getActiveDeploymentName()) && isCreateNewDeployment) {
            operations.add(String.format(CONFIRM_PROMPT_ACTIVATE_DEPLOYMENT, TextUtils.blue(deploymentClient.getDeploymentName()),
                    TextUtils.blue(deploymentClient.getAppName())));
        }
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

    protected boolean checkConfiguration() {
        final String pluginKey = plugin.getPluginLookupKey();
        final Xpp3Dom pluginDom = MavenUtils.getPluginConfiguration(project, pluginKey);
        if (pluginDom == null || pluginDom.getChildren().length == 0) {
            getLog().warn(PROJECT_NO_CONFIGURATION);
            return false;
        } else {
            return true;
        }
    }

    protected boolean isResourceSpecified(SpringConfiguration springConfiguration) {
        final Deployment deploymentConfiguration = springConfiguration.getDeployment();
        return deploymentConfiguration.getResources() != null && deploymentConfiguration.getResources().size() > 0;
    }

    protected void traceTelemetry(SpringAppClient springAppClient, SpringDeploymentClient deploymentClient,
                                  SpringConfiguration springConfiguration) {
        traceAuth();
        traceConfiguration(springConfiguration);
        traceDeployment(springAppClient, deploymentClient, springConfiguration);
    }

    protected void traceDeployment(SpringAppClient springAppClient, SpringDeploymentClient deploymentClient,
                                   SpringConfiguration springConfiguration) {
        final boolean isNewApp = springAppClient.getApp() == null;
        final boolean isNewDeployment = deploymentClient.getDeployment() == null;
        final boolean isDeploymentNameGiven = springConfiguration.getDeployment() != null &&
                StringUtils.isNotEmpty(springConfiguration.getDeployment().getDeploymentName());
        telemetries.put(TELEMETRY_KEY_IS_CREATE_NEW_APP, String.valueOf(isNewApp));
        telemetries.put(TELEMETRY_KEY_IS_CREATE_DEPLOYMENT, String.valueOf(isNewDeployment));
        telemetries.put(TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN, String.valueOf(isDeploymentNameGiven));
    }
}
