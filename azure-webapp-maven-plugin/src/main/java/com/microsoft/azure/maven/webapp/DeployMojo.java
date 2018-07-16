/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.deployadapter.DeploymentSlotAdapter;
import com.microsoft.azure.maven.webapp.deployadapter.IDeployTargetAdapter;
import com.microsoft.azure.maven.webapp.deployadapter.WebAppAdapter;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.concurrent.TimeUnit;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    public static final String DEPLOY_START = "Deploying to %s '%s'...";
    public static final String DEPLOY_SUCCESS = "Successfully deployed %s at 'https://%s'";
    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App.";
    public static final String UPDATE_WEBAPP = "Updating target Web App...";
    public static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App.";
    public static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    public static final String START_APP = "Starting Web App after deploying artifacts...";
    public static final String STOP_APP_DONE = "Successfully stopped Web App.";
    public static final String START_APP_DONE = "Successfully started Web App.";
    public static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    public static final String SLOT_SHOULD_EXIST_NOW = "Target deployment slot still does not exist." +
        "Please check if any error message during creation";

    protected DeploymentUtil util = new DeploymentUtil();

    @Override
    protected void doExecute() throws Exception {
        createOrUpdateWebApp();
        deployArtifacts();
    }

    protected void createOrUpdateWebApp() throws Exception {
        final WebApp app = getWebApp();
        if (app == null && this.isDeployToDeploymentSlot()) {
            throw new MojoExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
        }
        if (app == null) {
            createWebApp();
        } else {
            updateWebApp(app);
        }
    }

    protected void createWebApp() throws Exception {
        info(WEBAPP_NOT_EXIST);

        final WithCreate withCreate = getFactory().getRuntimeHandler(this).defineAppWithRuntime();
        getFactory().getSettingsHandler(this).processSettings(withCreate);
        withCreate.create();

        info(WEBAPP_CREATED);
    }

    protected void updateWebApp(final WebApp app) throws Exception {
        info(UPDATE_WEBAPP);

        final Update update = getFactory().getRuntimeHandler(this).updateAppRuntime(app);
        getFactory().getSettingsHandler(this).processSettings(update);
        update.apply();

        info(UPDATE_WEBAPP_DONE);

        if (isDeployToDeploymentSlot()) {
            getFactory().getDeploymentSlotHandler(this).createDeploymentSlotIfNotExist();
        }
    }

    protected void deployArtifacts() throws Exception {
        try {
            util.beforeDeployArtifacts();
            final IDeployTargetAdapter target = getDeployTarget();

            getLog().info(String.format(DEPLOY_START, target.getType(), target.getName()));

            getFactory().getArtifactHandler(this).publish(target);

            getLog().info(String.format(DEPLOY_SUCCESS, target.getType(), target.getDefaultHostName()));
        } finally {
            util.afterDeployArtifacts();
        }
    }

    protected IDeployTargetAdapter getDeployTarget() throws AzureAuthFailureException, MojoExecutionException {
        final WebApp app = getWebApp();
        if (this.isDeployToDeploymentSlot()) {
            final String slotName = getDeploymentSlotSetting().getSlotName();
            final DeploymentSlot slot = getDeploymentSlot(app, slotName);
            if (slot == null) {
                throw new MojoExecutionException(SLOT_SHOULD_EXIST_NOW);
            }
            return new DeploymentSlotAdapter(slot);
        }
        return new WebAppAdapter(app);
    }

    protected HandlerFactory getFactory() {
        return HandlerFactory.getInstance();
    }

    class DeploymentUtil {
        boolean isAppStopped = false;

        public void beforeDeployArtifacts() throws Exception {
            if (isStopAppDuringDeployment()) {
                info(STOP_APP);

                getWebApp().stop();

                // workaround for the resources release problem.
                // More details: https://github.com/Microsoft/azure-maven-plugins/issues/191
                TimeUnit.SECONDS.sleep(10 /* 10 seconds */);

                isAppStopped = true;

                info(STOP_APP_DONE);
            }
        }

        public void afterDeployArtifacts() throws Exception {
            if (isAppStopped) {
                info(START_APP);

                getWebApp().start();
                isAppStopped = false;

                info(START_APP_DONE);
            }
        }
    }
}
