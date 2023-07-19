/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.microsoft.azure.maven.prompt.DefaultPrompter;
import com.microsoft.azure.maven.prompt.IPrompter;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudClusterConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Deploy your project to target Azure Spring app. If target app doesn't exist, it will be created.
 */
@Mojo(name = "deploy")
@Slf4j
public class DeployMojo extends AbstractMojoBase {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    private static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    private static final String PROJECT_NOT_SUPPORT = "`azure-spring-apps:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
            "you can check the app status from Azure Portal.";
    private static final String CONFIRM_PROMPT_START = "`azure-spring-apps:deploy` will perform the following tasks";
    private static final String CONFIRM_PROMPT_CONFIRM = "Perform the above tasks? (Y/n):";

    /**
     * Boolean flag to control whether to wait the deployment status to be ready after deployment
     */
    @Parameter(property = "noWait")
    private Boolean noWait;

    /**
     * Boolean flag to control whether to prompt the tasks before deployment
     */
    @Parameter(property = "prompt")
    private Boolean prompt;

    @Override
    @AzureOperation("user/springcloud.deploy_mojo")
    protected void doExecute() throws Throwable {
        this.mergeCommandLineConfig();
        // set up account and select subscription here in `deploy`, since in some cases, `config` will not need to sign in
        loginAzure();
        selectSubscription();

        // Init spring clients, and prompt users to confirm
        final SpringCloudAppConfig appConfig = this.getConfiguration();
        final SpringCloudClusterConfig clusterConfig = appConfig.getCluster();
        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final File file = Optional.ofNullable(deploymentConfig).map(SpringCloudDeploymentConfig::getArtifact).map(IArtifact::getFile)
                .orElseThrow(() -> new AzureToolkitRuntimeException("No artifact is specified to deploy."));
        final String javaVersion = Optional.ofNullable(deploymentConfig).map(SpringCloudDeploymentConfig::getJavaVersion)
                .map(version -> StringUtils.removeStart(version, "Java_")).orElse(StringUtils.EMPTY);
        final SpringCloudCluster springCloudCluster = Azure.az(AzureSpringCloud.class).clusters(clusterConfig.getSubscriptionId()).get(clusterConfig.getClusterName(), clusterConfig.getResourceGroup());
        final Sku sku = Optional.ofNullable(springCloudCluster).map(SpringCloudCluster::getSku).orElseGet(() -> Sku.fromString(clusterConfig.getSku()));
        Objects.requireNonNull(sku, "Sku is required for creating Azure Spring Apps.");
        if (!sku.isEnterpriseTier()) {
            validateArtifactCompileVersion(javaVersion, file, getFailsOnRuntimeValidationError());
        }
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(appConfig, true, true);

        final List<AzureTask<?>> tasks = task.getSubTasks();
        final boolean shouldSkipConfirm = !BooleanUtils.isTrue(prompt) || (this.settings != null && !this.settings.isInteractiveMode());
        if (!shouldSkipConfirm && !this.confirm(tasks)) {
            log.warn("Deployment is cancelled!");
            return;
        }
        final SpringCloudDeployment deployment = task.doExecute();
        if (!BooleanUtils.isTrue(noWait) && Optional.of(deploymentConfig).map(SpringCloudDeploymentConfig::getArtifact).map(IArtifact::getFile).isPresent()) {
            if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                log.warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
            }
        }
        printStatus(deployment);
        printPublicUrl(deployment.getParent());
    }

    private void mergeCommandLineConfig() {
        try {
            final JavaPropsMapper mapper = new JavaPropsMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            final DeployMojo commandLineConfig = mapper.readSystemPropertiesAs(JavaPropsSchema.emptySchema(), DeployMojo.class);
            com.microsoft.azure.toolkit.lib.common.utils.Utils.copyProperties(this, commandLineConfig, false);
        } catch (IOException | IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("failed to merge command line configuration", e);
        }
    }

    protected boolean confirm(List<AzureTask<?>> tasks) throws MojoFailureException {
        try (final IPrompter prompter = new DefaultPrompter()) {
            System.out.println(CONFIRM_PROMPT_START);
            tasks.stream().map(AzureTask::getDescription).filter(t -> Objects.nonNull(t) && StringUtils.isNotBlank(t.toString()))
                .forEach((t) -> System.out.printf("\t- %s%n", t));
            return prompter.promoteYesNo(CONFIRM_PROMPT_CONFIRM, true, true);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected void printPublicUrl(SpringCloudApp app) {
        if (!app.isPublicEndpointEnabled()) {
            return;
        }
        log.info("Getting public url of app({})...", TextUtils.cyan(app.getName()));
        String publicUrl = app.getApplicationUrl();
        if (!BooleanUtils.isTrue(noWait) && StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.pollUntil(() -> {
                app.refresh();
                return app.getApplicationUrl();
            }, StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            log.warn("Failed to get application url");
        } else {
            log.info("Application url: {}", TextUtils.cyan(publicUrl));
        }
    }

    protected void printStatus(SpringCloudDeployment deployment) {
        log.info("Deployment Status: {}", color(deployment.getStatus()));
        deployment.getInstances().forEach(instance ->
                log.info(String.format("  InstanceName:%-10s  Status:%-10s Reason:%-10s DiscoverStatus:%-10s",
                        instance.getName(), color(instance.getStatus()), instance.getRemote().reason(), instance.getDiscoveryStatus())));
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

    @SneakyThrows
    @Override
    protected boolean isSkipMojo() {
        return !checkProjectPackaging(project) || !checkConfiguration();
    }
}
