/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudJavaVersion;
import com.microsoft.azure.tools.utils.RxUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractMojoBase {

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

    @Parameter(property = "noWait")
    private boolean noWait;

    @Parameter(property = "prompt")
    private boolean prompt;

    @Override
    protected void doExecute() throws MojoExecutionException, AzureExecutionException {
        if (!checkProjectPackaging(project) || !checkConfiguration()) {
            return;
        }
        // Init spring clients, and prompt users to confirm
        final SpringCloudAppConfig appConfig = this.getConfiguration();
        final File artifact = isArtifactsSpecified(appConfig) ?
            getArtifactFromConfiguration(appConfig) : MavenArtifactUtils.getArtifactFromTargetFolder(project);
        final boolean enableDisk = appConfig.getDeployment() != null && appConfig.getDeployment().isEnablePersistentStorage();
        final String clusterName = appConfig.getClusterName();
        final String appName = appConfig.getAppName();

        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final Map<String, String> env = deploymentConfig.getEnvironment();
        final String jvmOptions = deploymentConfig.getJvmOptions();
        final ScaleSettings scaleSettings = deploymentConfig.getScaleSettings();
        final String runtimeVersion = deploymentConfig.getJavaVersion();
        final String deploymentName = Optional.ofNullable(deploymentConfig.getDeploymentName()).orElse("default");

        final AzureSpringCloud az = AzureSpringCloud.az(this.getClient());
        final SpringCloudCluster cluster = az.cluster(clusterName);
        final SpringCloudApp app = cluster.app(appName);
        final SpringCloudDeployment deployment = app.deployment(deploymentName);

        if (!app.exists()) {
            app.create().commit();
        }

        final String artifactPath = app.uploadArtifact(artifact.getPath());

        (!deployment.exists() ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configAppArtifactPath(artifactPath)
            .commit();

        app.update()
            .activate(Optional.ofNullable(appConfig.getActiveDeploymentName()).orElse(deploymentName))
            .setPublic(appConfig.isPublic())
            .enablePersistentDisk(enableDisk)
            .commit();

        if (!noWait) {
            deployment.waitUntilReady(GET_STATUS_TIMEOUT);
        }
        printStatus(deployment);
        printPublicUrl(app);
    }

    protected void printPublicUrl(SpringCloudApp app) {
        if (!app.entity().isPublic()) {
            return;
        }
        getLog().info(GETTING_PUBLIC_URL);
        String publicUrl = app.entity().getApplicationUrl();
        if (!noWait && StringUtils.isEmpty(publicUrl)) {
            publicUrl = RxUtils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNoneEmpty, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            getLog().warn(GET_APP_URL_FAIL);
        } else {
            getLog().info(String.format(GET_APP_URL_SUCCESSFULLY, publicUrl));
        }
    }

    protected void printStatus(SpringCloudDeployment deployment) {
        if (!AzureSpringCloudConfigUtils.isDeploymentDone(deployment)) {
            getLog().warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
        }
        System.out.printf("Deployment Status: %s%n", deployment.entity().getStatus());
        deployment.entity().getInstances().forEach(instance ->
            System.out.printf("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s%n",
                instance.name(), instance.status(), instance.reason(), instance.discoveryStatus()));
    }

    protected boolean checkProjectPackaging(MavenProject project) throws MojoExecutionException {
        if (MavenConfigUtils.isJarPackaging(project)) {
            return true;
        } else if (MavenConfigUtils.isPomPackaging(project)) {
            getLog().info(PROJECT_SKIP);
            return false;
        } else {
            throw new MojoExecutionException(String.format(PROJECT_NOT_SUPPORT, project.getPackaging()));
        }
    }

    protected boolean checkConfiguration() {
        final String pluginKey = plugin.getPluginLookupKey();
        final Xpp3Dom pluginDom = MavenConfigUtils.getPluginConfiguration(project, pluginKey);
        if (pluginDom == null || pluginDom.getChildren().length == 0) {
            getLog().warn(PROJECT_NO_CONFIGURATION);
            return false;
        } else {
            return true;
        }
    }

    protected boolean isArtifactsSpecified(SpringCloudAppConfig springConfiguration) {
        final SpringCloudDeploymentConfig deploymentConfig = springConfiguration.getDeployment();
        return deploymentConfig.getArtifacts() != null && deploymentConfig.getArtifacts().size() > 0;
    }

    private static File getArtifactFromConfiguration(SpringCloudAppConfig springConfiguration) throws MojoExecutionException {
        final SpringCloudDeploymentConfig deploymentConfig = springConfiguration.getDeployment();
        final List<File> files = deploymentConfig.getArtifacts();
        return MavenArtifactUtils.getExecutableJarFiles(files);
    }
}
