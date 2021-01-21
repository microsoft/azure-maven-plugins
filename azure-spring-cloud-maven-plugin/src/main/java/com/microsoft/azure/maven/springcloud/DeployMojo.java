/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.common.prompt.DefaultPrompter;
import com.microsoft.azure.common.prompt.IPrompter;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.tools.utils.RxUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils.DEFAULT_DEPLOYMENT_NAME;

@Mojo(name = "deploy")
public class DeployMojo extends AbstractMojoBase {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    private static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    private static final String PROJECT_NOT_SUPPORT = "`azure-spring-cloud:deploy` does not support maven project with " +
        "packaging %s, only jar is supported";
    private static final String CREATE_APP_DOING = "Creating the app...";
    private static final String CREATE_APP_DONE = "Successfully created the app.";
    private static final String DEPLOYMENT_STORAGE_STATUS = "Persistent storage path : %s, size : %s GB.";
    private static final String UPLOAD_ARTIFACT_DOING = "Uploading artifacts...";
    private static final String UPLOAD_ARTIFACT_DONE = "Successfully uploaded the artifacts.";
    private static final String CREATE_DEPLOYMENT_DOING = "Creating the deployment...";
    private static final String CREATE_DEPLOYMENT_DONE = "Successfully created the deployment.";
    private static final String UPDATE_DEPLOYMENT_DOING = "Updating the deployment...";
    private static final String UPDATE_DEPLOYMENT_DONE = "Successfully updated the deployment.";
    private static final String UPDATE_APP_DOING = "Updating the app...";
    private static final String UPDATE_APP_DONE = "Successfully updated the app.";
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_DOING = "Getting deployment status...";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String GET_APP_URL_DOING = "Getting public url...";
    private static final String GET_APP_URL_SUCCESS = "Application url : %s";
    private static final String GET_APP_URL_FAILURE = "Failed to get application url";
    private static final String CONFIRM_PROMPT_START = "`azure-spring-cloud:deploy` will perform the following tasks";
    private static final String CONFIRM_PROMPT_CONFIRM = "Perform the above tasks? (Y/n):";

    @Parameter(property = "noWait")
    private boolean noWait;

    @Parameter(property = "prompt")
    private boolean prompt;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
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
        final String deploymentName = Optional.ofNullable(deploymentConfig.getDeploymentName()).orElse(DEFAULT_DEPLOYMENT_NAME);

        final AzureSpringCloud az = AzureSpringCloud.az(this.getAppPlatformManager());
        final SpringCloudCluster cluster = az.cluster(clusterName);
        final SpringCloudApp app = cluster.app(appName);
        final SpringCloudDeployment deployment = app.deployment(deploymentName);

        final String CREATE_APP_TITLE = String.format("create new app(%s) on service(%s)", appName, clusterName);
        final String UPDATE_APP_TITLE = String.format("update app(%s) of service(%s)", appName, clusterName);
        final String CREATE_DEPLOYMENT_TITLE = String.format("create new deployment(%s) in app(%s)", deploymentName, appName);
        final String UPDATE_DEPLOYMENT_TITLE = String.format("update deployment(%s) of app(%s)", deploymentName, appName);
        final String UPLOAD_ARTIFACT_TITLE = String.format("upload artifact(%s) to app(%s)", artifact.getPath(), appName);

        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !deployment.exists();
        final List<AzureTask<?>> tasks = new ArrayList<>();
        final SpringCloudApp.Creator appCreator = app.create();
        final SpringCloudApp.Uploader artifactUploader = app.uploadArtifact(artifact.getPath());
        final SpringCloudDeployment.Updater deploymentModifier = (toCreateDeployment ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configArtifact(artifactUploader.getArtifact());
        final SpringCloudApp.Updater appUpdater = app.update()
            .activate(Optional.ofNullable(appConfig.getActiveDeploymentName()).orElse(deploymentName))
            .setPublic(appConfig.isPublic())
            .enablePersistentDisk(enableDisk);

        if (toCreateApp) {
            tasks.add(new AzureTask<Void>(CREATE_APP_TITLE, () -> {
                getLog().info(CREATE_APP_DOING);
                appCreator.commit();
                getLog().info(CREATE_APP_DONE);
            }));
        }
        tasks.add(new AzureTask<Void>(UPLOAD_ARTIFACT_TITLE, () -> {
            getLog().info(UPLOAD_ARTIFACT_DOING);
            artifactUploader.commit();
            getLog().info(UPLOAD_ARTIFACT_DONE);
        }));
        tasks.add(new AzureTask<Void>(toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE, () -> {
            getLog().info(toCreateDeployment ? CREATE_DEPLOYMENT_DOING : UPDATE_DEPLOYMENT_DOING);
            deploymentModifier.commit();
            getLog().info(toCreateDeployment ? CREATE_DEPLOYMENT_DONE : UPDATE_DEPLOYMENT_DONE);
        }));
        if (!appUpdater.isSkippable()) {
            tasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, () -> {
                getLog().info(UPDATE_APP_DOING);
                appUpdater.commit();
                getLog().info(UPDATE_APP_DONE);
                getLog().warn(UPDATE_APP_WARNING);
            }));
        }

        tasks.add(new AzureTask<Void>(() -> {
            if (!noWait) {
                getLog().info(GET_DEPLOYMENT_STATUS_DOING);
                if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                    getLog().warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
                }
            }
            printStatus(deployment);
            printPublicUrl(app);
        }));
        if (this.confirmDeploy(tasks)) {
            tasks.forEach(AzureTask::execute);
        }
    }

    protected boolean confirmDeploy(List<AzureTask<?>> tasks) throws MojoFailureException {
        try (IPrompter prompter = new DefaultPrompter()) {
            getLog().info(CONFIRM_PROMPT_START);
            tasks.stream().filter(t -> StringUtils.isNotBlank(t.getTitle())).forEach((t) -> System.out.printf("\t*  %s%n", t.getTitle()));
            return prompter.promoteYesNo(CONFIRM_PROMPT_CONFIRM, true, true);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected void printPublicUrl(SpringCloudApp app) {
        if (!app.entity().isPublic()) {
            return;
        }
        getLog().info(GET_APP_URL_DOING);
        String publicUrl = app.entity().getApplicationUrl();
        if (!noWait && StringUtils.isEmpty(publicUrl)) {
            publicUrl = RxUtils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            getLog().warn(GET_APP_URL_FAILURE);
        } else {
            getLog().info(String.format(GET_APP_URL_SUCCESS, publicUrl));
        }
    }

    protected void printStatus(SpringCloudDeployment deployment) {
        if (!AzureSpringCloudConfigUtils.isDeploymentDone(deployment)) {
            getLog().warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
        }
        getLog().info(String.format("Deployment Status: %s", deployment.entity().getStatus()));
        deployment.entity().getInstances().forEach(instance ->
            getLog().info(String.format("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s",
                instance.name(), instance.status(), instance.reason(), instance.discoveryStatus())));
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
