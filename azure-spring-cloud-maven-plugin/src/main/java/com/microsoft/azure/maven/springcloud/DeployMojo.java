/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.maven.prompt.DefaultPrompter;
import com.microsoft.azure.maven.prompt.IPrompter;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;
import java.util.List;

@Mojo(name = "deploy")
@Slf4j
public class DeployMojo extends AbstractMojoBase {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String PROJECT_SKIP = "Packaging type is pom, taking no actions.";
    private static final String PROJECT_NO_CONFIGURATION = "Configuration does not exist, taking no actions.";
    private static final String PROJECT_NOT_SUPPORT = "`azure-spring-cloud:deploy` does not support maven project with " +
            "packaging %s, only jar is supported";
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
    @AzureOperation(name = "springcloud|app.create_update_mojo", type = AzureOperation.Type.ACTION)
    protected void doExecute() {
        if (!checkProjectPackaging(project) || !checkConfiguration()) {
            return;
        }
        // Init spring clients, and prompt users to confirm
        final SpringCloudAppConfig appConfig = this.getConfiguration();
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(appConfig);

        final List<AzureTask<?>> tasks = task.getSubTasks();
        final boolean shouldSkipConfirm = !prompt || (this.settings != null && !this.settings.isInteractiveMode());
        if (!shouldSkipConfirm && !this.confirm(tasks)) {
            log.warn("Deployment is cancelled!");
            return;
        }
        final SpringCloudDeployment deployment = task.execute();
        if (!noWait) {
            if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                log.warn(GET_DEPLOYMENT_STATUS_TIMEOUT);
            }
        }
        printStatus(deployment);
        printPublicUrl(deployment.app());
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
            publicUrl = Utils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            log.warn("Failed to get application url");
        } else {
            log.info("Application url: {}", TextUtils.cyan(publicUrl));
        }
    }

    protected void printStatus(SpringCloudDeployment deployment) {
        log.info("Deployment Status: {}", color(deployment.entity().getStatus().toString()));
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
