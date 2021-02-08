/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.common.prompt.DefaultPrompter;
import com.microsoft.azure.common.prompt.IPrompter;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.tools.utils.RxUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils.DEFAULT_DEPLOYMENT_NAME;

@Mojo(name = "deploy")
@Slf4j
public class DeployMojo extends AbstractMojoBase {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    private static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    private static final String PROJECT_NOT_SUPPORT = "`azure-spring-cloud:deploy` does not support maven project with " +
        "packaging %s, only jar is supported";
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String CONFIRM_PROMPT_START = "`azure-spring-cloud:deploy` will perform the following tasks";
    private static final String CONFIRM_PROMPT_CONFIRM = "Perform the above tasks? (Y/n):";

    @Parameter(property = "noWait")
    private boolean noWait;

    @Parameter(property = "prompt")
    private boolean prompt;

    @SneakyThrows
    @Override
    protected void doExecute() {
        if (!checkProjectPackaging(project) || !checkConfiguration()) {
            return;
        }
        // Init spring clients, and prompt users to confirm
        final SpringCloudAppConfig appConfig = this.getConfiguration();
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final IArtifact artifact = deploymentConfig.getArtifact();
        final File file = Objects.nonNull(artifact) ? artifact.getFile() : MavenArtifactUtils.getArtifactFromTargetFolder(project);
        final boolean enableDisk = appConfig.getDeployment() != null && appConfig.getDeployment().isEnablePersistentStorage();
        final String clusterName = appConfig.getClusterName();
        final String appName = appConfig.getAppName();

        final Map<String, String> env = deploymentConfig.getEnvironment();
        final String jvmOptions = deploymentConfig.getJvmOptions();
        final ScaleSettings scaleSettings = deploymentConfig.getScaleSettings();
        final String runtimeVersion = deploymentConfig.getJavaVersion();

        final SpringCloudCluster cluster = Azure.az(AzureSpringCloud.class).cluster(clusterName);
        final SpringCloudApp app = cluster.app(appName);
        final String deploymentName = StringUtils.firstNonBlank(
            deploymentConfig.getDeploymentName(),
            appConfig.getActiveDeploymentName(),
            app.getActiveDeploymentName(),
            DEFAULT_DEPLOYMENT_NAME
        );
        final SpringCloudDeployment deployment = app.deployment(deploymentName);

        final String CREATE_APP_TITLE = String.format("Create new app(%s) on service(%s)", TextUtils.cyan(appName), TextUtils.cyan(clusterName));
        final String UPDATE_APP_TITLE = String.format("Update app(%s) of service(%s)", TextUtils.cyan(appName), TextUtils.cyan(clusterName));
        final String CREATE_DEPLOYMENT_TITLE = String.format("Create new deployment(%s) in app(%s)", TextUtils.cyan(deploymentName), TextUtils.cyan(appName));
        final String UPDATE_DEPLOYMENT_TITLE = String.format("Update deployment(%s) of app(%s)", TextUtils.cyan(deploymentName), TextUtils.cyan(appName));
        final String UPLOAD_ARTIFACT_TITLE = String.format("Upload artifact(%s) to app(%s)", TextUtils.cyan(file.getPath()), TextUtils.cyan(appName));

        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !deployment.exists();

        traceDeployment(toCreateApp, toCreateDeployment, appConfig);

        final List<AzureTask<?>> tasks = new ArrayList<>();
        final SpringCloudApp.Creator appCreator = app.create();
        final SpringCloudApp.Uploader artifactUploader = app.uploadArtifact(file.getPath());
        final SpringCloudDeployment.Updater deploymentModifier = (toCreateDeployment ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configArtifact(artifactUploader.getArtifact());
        final SpringCloudApp.Updater appUpdater = app.update()
            // active deployment should keep active.
            .activate(StringUtils.firstNonBlank(app.getActiveDeploymentName(), toCreateDeployment ? deploymentName : null))
            .setPublic(appConfig.isPublic())
            .enablePersistentDisk(enableDisk);

        if (toCreateApp) {
            tasks.add(new AzureTask<Void>(CREATE_APP_TITLE, () -> {
                log.info("Creating app({})...", TextUtils.cyan(appName));
                appCreator.commit();
                log.info("Successfully created the app.");
            }));
        }
        tasks.add(new AzureTask<Void>(UPLOAD_ARTIFACT_TITLE, () -> {
            log.info("Uploading artifact({}) to Azure...", TextUtils.cyan(file.getPath()));
            artifactUploader.commit();
            log.info("Successfully uploaded the artifact.");
        }));
        tasks.add(new AzureTask<Void>(toCreateDeployment ? CREATE_DEPLOYMENT_TITLE : UPDATE_DEPLOYMENT_TITLE, () -> {
            log.info(toCreateDeployment ? "Creating deployment({})..." : "Updating deployment({})...", TextUtils.cyan(deploymentName));
            deploymentModifier.commit();
            log.info(toCreateDeployment ? "Successfully created the deployment" : "Successfully updated the deployment");
        }));
        if (!appUpdater.isSkippable()) {
            tasks.add(new AzureTask<Void>(UPDATE_APP_TITLE, () -> {
                log.info("Updating app({})...", TextUtils.cyan(appName));
                appUpdater.commit();
                log.info("Successfully updated the app.");
                log.warn(UPDATE_APP_WARNING);
            }));
        }

        tasks.add(new AzureTask<Void>(() -> {
            if (!noWait) {
                log.info("Getting deployment status...");
                if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                    log.warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
                }
            }
            printStatus(deployment);
            printPublicUrl(app);
        }));
        final boolean shouldSkipConfirm = !prompt || (this.settings != null && !this.settings.isInteractiveMode());
        if (!shouldSkipConfirm && !this.confirm(tasks)) {
            log.warn("Deployment is cancelled!");
            return;
        }
        tasks.forEach((task) -> task.getSupplier().get());
    }

    protected boolean confirm(List<AzureTask<?>> tasks) throws MojoFailureException {
        try {
            final IPrompter prompter = new DefaultPrompter();
            System.out.println(CONFIRM_PROMPT_START);
            tasks.stream().filter(t -> StringUtils.isNotBlank(t.getTitle().toString())).forEach((t) -> System.out.printf("\t- %s%n", t.getTitle()));
            return prompter.promoteYesNo(CONFIRM_PROMPT_CONFIRM, true, true);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected void printPublicUrl(SpringCloudApp app) {
        if (!app.entity().isPublic()) {
            return;
        }
        log.info("Getting public url of app({})...", TextUtils.cyan(app.name()));
        String publicUrl = app.entity().getApplicationUrl();
        if (!noWait && StringUtils.isEmpty(publicUrl)) {
            publicUrl = RxUtils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            log.warn("Failed to get application url");
        } else {
            log.info("Application url: {}", TextUtils.cyan(publicUrl));
        }
    }

    protected void printStatus(SpringCloudDeployment deployment) {
        final DeploymentResourceStatus status = deployment.entity().getStatus();
        log.info("Deployment Status: {}", color(status.toString()));
        deployment.entity().getInstances().forEach(instance ->
            log.info(String.format("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s",
                instance.name(), color(instance.status()), instance.reason(), instance.discoveryStatus())));
    }

    private static String color(String status) {
        switch (status.toUpperCase()) {
            case "RUNNING":
                return TextUtils.green(status);
            case "FAILED":
            case "STOPPED":
                return TextUtils.red(status);
            case "UNKNOWN":
                return status;
            default:
                return TextUtils.blue(status);
        }
    }

    protected boolean checkProjectPackaging(MavenProject project) throws MojoExecutionException {
        if (MavenConfigUtils.isJarPackaging(project)) {
            return true;
        } else if (MavenConfigUtils.isPomPackaging(project)) {
            log.info(PROJECT_SKIP);
            return false;
        } else {
            throw new MojoExecutionException(String.format(PROJECT_NOT_SUPPORT, project.getPackaging()));
        }
    }

    protected boolean checkConfiguration() {
        final String pluginKey = plugin.getPluginLookupKey();
        final Xpp3Dom pluginDom = MavenConfigUtils.getPluginConfiguration(project, pluginKey);
        if (pluginDom == null || pluginDom.getChildren().length == 0) {
            log.warn(PROJECT_NO_CONFIGURATION);
            return false;
        } else {
            return true;
        }
    }
}
