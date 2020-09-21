/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.prompt.DefaultPrompter;
import com.microsoft.azure.common.prompt.IPrompter;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.PersistentDisk;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ResourceUploadDefinitionInner;
import com.microsoft.azure.maven.common.utils.MavenUtils;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.azure.maven.spring.spring.SpringAppClient;
import com.microsoft.azure.maven.spring.spring.SpringDeploymentClient;
import com.microsoft.azure.maven.spring.utils.Utils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_DEPLOYMENT;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_NEW_APP;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractSpringMojo {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String GETTING_DEPLOYMENT_STATUS = "Getting deployment status...";
    private static final String GETTING_PUBLIC_URL = "Getting public url...";
    private static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    private static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    private static final String PROJECT_NOT_SUPPORT = "`azure-spring-cloud:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
    private static final String GET_APP_URL_SUCCESSFULLY = "Application url : %s";
    private static final String GET_APP_URL_FAIL = "Fail to get application url";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
            "you can check the app status from Azure Portal.";
    private static final String STATUS_CREATE_APP = "Creating the app...";
    private static final String STATUS_CREATE_APP_DONE = "Successfully created the app.";
    private static final String STATUS_UPDATE_APP = "Updating the app...";
    private static final String STATUS_UPDATE_APP_DONE = "Successfully updated the app.";
    private static final String STATUS_UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String STATUS_CREATE_DEPLOYMENT = "Creating the deployment...";
    private static final String STATUS_CREATE_DEPLOYMENT_DONE = "Successfully created the deployment.";
    private static final String STATUS_UPDATE_DEPLOYMENT = "Updating the deployment...";
    private static final String STATUS_UPDATE_DEPLOYMENT_DONE = "Successfully updated the deployment.";
    private static final String DEPLOYMENT_STORAGE_STATUS = "Persistent storage path : %s, size : %s GB.";
    private static final String STATUS_UPLOADING_ARTIFACTS = "Uploading artifacts...";
    private static final String STATUS_UPLOADING_ARTIFACTS_DONE = "Successfully uploaded the artifacts.";
    private static final String CONFIRM_PROMPT_START = "`azure-spring-cloud:deploy` will perform the following tasks";
    private static final String CONFIRM_PROMPT_CREATE_NEW_APP = "Create new app [%s]";
    private static final String CONFIRM_PROMPT_UPDATE_APP = "Update app [%s]";
    private static final String CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT = "Create new deployment [%s] in app [%s]";
    private static final String CONFIRM_PROMPT_UPDATE_DEPLOYMENT = "Update deployment [%s] in app [%s]";
    private static final String CONFIRM_PROMPT_ACTIVATE_DEPLOYMENT = "Set [%s] as the active deployment of app [%s]";
    private static final String CONFIRM_PROMPT_CONFIRM = "Perform the above tasks? (Y/n):";

    protected static final List<DeploymentResourceStatus> DEPLOYMENT_PROCESSING_STATUS =
            Arrays.asList(DeploymentResourceStatus.COMPILING, DeploymentResourceStatus.ALLOCATING, DeploymentResourceStatus.UPGRADING);

    @Parameter(property = "noWait")
    private boolean noWait;

    @Parameter(property = "prompt")
    private boolean prompt;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException, AzureExecutionException {
        if (!checkProjectPackaging(project) || !checkConfiguration()) {
            return;
        }
        // Init spring clients, and prompt users to confirm
        final SpringConfiguration configuration = this.getConfiguration();
        final Deployment deploymentConfiguration = configuration.getDeployment();
        final SpringAppClient springAppClient = getSpringServiceClient().newSpringAppClient(configuration);
        AppResourceInner app = springAppClient.getApp();
        final String deploymentName = deploymentConfiguration.getDeploymentName();
        final SpringDeploymentClient deploymentClient = springAppClient.getDeploymentClient(deploymentName, app);
        DeploymentResourceInner deployment = deploymentClient.getDeployment();
        final boolean toCreateNewDeployment = deployment == null;
        if (!shouldSkipConfirm() && !confirmDeploy(app, deployment, configuration)) {
            getLog().info("Terminate deployment");
            return;
        }
        // Prepare telemetries
        traceTelemetry(app, deployment, configuration);
        // Create new App if not exist
        if (Objects.isNull(app)) {
            getLog().info(STATUS_CREATE_APP);
            app = springAppClient.createApp(configuration);
            getLog().info(STATUS_CREATE_APP_DONE);
        }
        final PersistentDisk persistentDisk = app.properties().persistentDisk();
        if (persistentDisk != null) {
            getLog().info(String.format(DEPLOYMENT_STORAGE_STATUS, persistentDisk.mountPath(), persistentDisk.sizeInGB()));
        }

        // Upload artifact
        // TODO: no need to re-upload if artifact is not changed
        getLog().info(STATUS_UPLOADING_ARTIFACTS);
        final File toDeploy = isResourceSpecified(configuration) ? Utils.getArtifactFromConfiguration(configuration) :
                Utils.getArtifactFromTargetFolder(project);
        final ResourceUploadDefinitionInner uploadDefinition = springAppClient.uploadArtifact(toDeploy);
        getLog().info(STATUS_UPLOADING_ARTIFACTS_DONE);

        // Create or update deployment
        if (Objects.isNull(deployment)) {
            getLog().info(STATUS_CREATE_DEPLOYMENT);
            deployment = deploymentClient.createDeployment(configuration.getDeployment(), uploadDefinition);
            getLog().info(STATUS_CREATE_DEPLOYMENT_DONE);
            final String activeDeploymentName = app.properties().activeDeploymentName();
            if (StringUtils.isAllEmpty(configuration.getActiveDeploymentName(), activeDeploymentName)) {
                configuration.withActiveDeploymentName(deployment.name());
            }
        }

        // TODO: no need to update if `deployment.properties` does include `configuration.getDeployment()`
        if (!toCreateNewDeployment) {
            getLog().info(STATUS_UPDATE_DEPLOYMENT);
            deploymentClient.updateDeployment(deployment, configuration.getDeployment(), uploadDefinition);
            getLog().info(STATUS_UPDATE_DEPLOYMENT_DONE);
        }

        // TODO: no need to update if `app.properties` does include `configuration`
        getLog().info(STATUS_UPDATE_APP);
        springAppClient.updateApp(app, configuration);
        getLog().info(STATUS_UPDATE_APP_DONE);

        getLog().warn(STATUS_UPDATE_APP_WARNING);

        // Showing deployment status and public url
        showDeploymentStatus(deploymentClient);
        showPublicUrl(springAppClient);
    }

    protected void showPublicUrl(SpringAppClient springAppClient) throws MojoExecutionException {
        if (!springAppClient.isPublic()) {
            return;
        }
        getLog().info(GETTING_PUBLIC_URL);
        String publicUrl = springAppClient.getApplicationUrl();
        if (!noWait && StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.getResourceWithPrediction(springAppClient::getApplicationUrl, StringUtils::isNoneEmpty, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            getLog().warn(GET_APP_URL_FAIL);
        } else {
            getLog().info(String.format(GET_APP_URL_SUCCESSFULLY, publicUrl));
        }
    }

    protected void showDeploymentStatus(SpringDeploymentClient springDeploymentClient) {
        getLog().info(GETTING_DEPLOYMENT_STATUS);
        DeploymentResourceInner deploymentResource = springDeploymentClient.getDeployment();
        if (!noWait && !isDeploymentDone(deploymentResource)) {
            deploymentResource = Utils.getResourceWithPrediction(springDeploymentClient::getDeployment, this::isDeploymentDone, GET_STATUS_TIMEOUT);
        }
        if (!isDeploymentDone(deploymentResource)) {
            getLog().warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
        }
        printDeploymentStatus(deploymentResource);
    }

    protected void printDeploymentStatus(DeploymentResourceInner deploymentResource) {
        System.out.println(String.format("Deployment Status: %s", deploymentResource.properties().status()));
        for (final DeploymentInstance instance : deploymentResource.properties().instances()) {
            System.out.println(String.format("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s", instance.name(), instance.status()
                    , instance.reason(), instance.discoveryStatus()));
        }
    }

    protected boolean isDeploymentDone(DeploymentResourceInner deploymentResource) {
        final DeploymentResourceStatus deploymentResourceStatus = deploymentResource.properties().status();
        if (DEPLOYMENT_PROCESSING_STATUS.contains(deploymentResourceStatus)) {
            return false;
        }
        final String finalDiscoverStatus = BooleanUtils.isTrue(deploymentResource.properties().active()) ? "UP" : "OUT_OF_SERVICE";
        final List<DeploymentInstance> instanceList = deploymentResource.properties().instances();
        final boolean isInstanceDeployed = instanceList.stream().noneMatch(instance ->
                StringUtils.equalsIgnoreCase(instance.status(), "waiting") || StringUtils.equalsIgnoreCase(instance.status(), "pending"));
        final boolean isInstanceDiscovered = instanceList.stream().allMatch(instance ->
                StringUtils.equalsIgnoreCase(instance.discoveryStatus(), finalDiscoverStatus));
        return isInstanceDeployed && isInstanceDiscovered;
    }

    protected boolean shouldSkipConfirm() {
        // Skip confirm when -Dprompt or in batch model
        return !prompt || (this.settings != null && !this.settings.isInteractiveMode());
    }

    protected boolean confirmDeploy(AppResourceInner app, DeploymentResourceInner deployment, SpringConfiguration configuration) throws MojoFailureException {
        try (IPrompter prompter = new DefaultPrompter()) {
            final List<String> operations = getOperations(app, deployment, configuration);
            System.out.println(CONFIRM_PROMPT_START);
            for (int i = 1; i <= operations.size(); i++) {
                System.out.println(String.format("%-2d  %s", i, operations.get(i - 1)));
            }
            return prompter.promoteYesNo(CONFIRM_PROMPT_CONFIRM, true, true);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected List<String> getOperations(AppResourceInner app, DeploymentResourceInner deployment, SpringConfiguration configuration) {
        final boolean isCreateNewApp = app == null;
        final boolean isCreateNewDeployment = deployment == null;
        final String appName = configuration.getAppName();
        final String deploymentName = configuration.getDeployment().getDeploymentName();
        final List<String> operations = new ArrayList<>();
        final String appPrompt = isCreateNewApp ?
                String.format(CONFIRM_PROMPT_CREATE_NEW_APP, TextUtils.blue(appName)) :
                String.format(CONFIRM_PROMPT_UPDATE_APP, TextUtils.blue(appName));
        operations.add(appPrompt);

        final String deploymentPrompt = isCreateNewDeployment ?
                String.format(CONFIRM_PROMPT_CREATE_NEW_DEPLOYMENT, TextUtils.blue(deploymentName),
                        TextUtils.blue(appName)) :
                String.format(CONFIRM_PROMPT_UPDATE_DEPLOYMENT, TextUtils.blue(deploymentName),
                        TextUtils.blue(appName));
        operations.add(deploymentPrompt);

        if (StringUtils.isEmpty(SpringAppClient.getActiveDeploymentName(app)) && isCreateNewDeployment) {
            operations.add(String.format(CONFIRM_PROMPT_ACTIVATE_DEPLOYMENT, TextUtils.blue(deploymentName),
                    TextUtils.blue(appName)));
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

    protected void traceTelemetry(AppResourceInner app, DeploymentResourceInner deployment, SpringConfiguration configuration) {
        traceAuth();
        traceConfiguration(configuration);
        traceDeployment(app, deployment, configuration);
    }

    protected void traceDeployment(AppResourceInner app, DeploymentResourceInner deployment, SpringConfiguration configuration) {
        final boolean isNewApp = app == null;
        final boolean isNewDeployment = deployment == null;
        final boolean isDeploymentNameGiven = configuration.getDeployment() != null &&
                StringUtils.isNotEmpty(configuration.getDeployment().getDeploymentName());
        telemetries.put(TELEMETRY_KEY_IS_CREATE_NEW_APP, String.valueOf(isNewApp));
        telemetries.put(TELEMETRY_KEY_IS_CREATE_DEPLOYMENT, String.valueOf(isNewDeployment));
        telemetries.put(TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN, String.valueOf(isDeploymentNameGiven));
    }
}
