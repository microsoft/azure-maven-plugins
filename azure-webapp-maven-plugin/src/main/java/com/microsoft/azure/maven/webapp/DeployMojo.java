/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.MavenUtility;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App.";
    public static final String CREATE_DEPLOYMENT_SLOT = "Target Deployment Slot doesn't exist. Creating a new one...";
    public static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created the Deployment Slot.";
    public static final String UPDATE_WEBAPP = "Updating target Web App...";
    public static final String UPDATE_WEBAPP_SKIP = "No runtime configured. Skip the update.";
    public static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App.";
    public static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    public static final String START_APP = "Starting Web App after deploying artifacts...";
    public static final String STOP_APP_DONE = "Successfully stopped Web App.";
    public static final String START_APP_DONE = "Successfully started Web App.";
    public static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    public static final String SLOT_SHOULD_EXIST_NOW = "Target deployment slot still does not exist. " +
            "Please check if any error message during creation";

    protected DeploymentUtil util = new DeploymentUtil();

    @Override
    protected void doExecute() throws Exception {
        // todo: use parser to getAzureClient from mojo configs
        final RuntimeHandler runtimeHandler = getFactory().getRuntimeHandler(
                getWebAppConfiguration(), getAzureClient());
        // todo: use parser to get web app from mojo configs
        final WebApp app = getWebApp();
        if (app == null) {
            if (this.isDeployToDeploymentSlot()) {
                throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
            }
            createWebApp(runtimeHandler);
        } else {
            updateWebApp(runtimeHandler, app);
        }
        deployArtifacts();
    }

    protected void createWebApp(final RuntimeHandler runtimeHandler) throws Exception {
        info(WEBAPP_NOT_EXIST);

        final WithCreate withCreate = (WithCreate) runtimeHandler.defineAppWithRuntime();
        getFactory().getSettingsHandler(this).processSettings(withCreate);
        withCreate.create();

        info(WEBAPP_CREATED);
    }

    protected void updateWebApp(final RuntimeHandler runtimeHandler, final WebApp app) throws Exception {
        // Update App Service Plan
        runtimeHandler.updateAppServicePlan(app);
        // Update Web App
        final Update update = (Update) runtimeHandler.updateAppRuntime(app);
        if (update == null) {
            info(UPDATE_WEBAPP_SKIP);
        } else {
            info(UPDATE_WEBAPP);
            getFactory().getSettingsHandler(this).processSettings(update);
            update.apply();
            info(UPDATE_WEBAPP_DONE);
        }

        if (isDeployToDeploymentSlot()) {
            info(CREATE_DEPLOYMENT_SLOT);

            getFactory().getDeploymentSlotHandler(this).createDeploymentSlotIfNotExist();

            info(CREATE_DEPLOYMENT_SLOT_DONE);
        }
    }

    protected void deployArtifacts() throws Exception {
        try {
            util.beforeDeployArtifacts();
            final WebApp app = getWebApp();
            final DeployTarget target;

            if (this.isDeployToDeploymentSlot()) {
                final String slotName = getDeploymentSlotSetting().getName();
                final DeploymentSlot slot = getDeploymentSlot(app, slotName);
                if (slot == null) {
                    throw new AzureExecutionException(SLOT_SHOULD_EXIST_NOW);
                }
                target = new DeploymentSlotDeployTarget(slot);
            } else {
                target = new WebAppDeployTarget(app);
            }


            if (this.getResources() == null || this.getResources().isEmpty()) {
                Log.warn("No <resources> is found in <deployment> element in pom.xml, skip deployment.");
                return;
            }

            copyArtifactsToStagingDirectory();

            getFactory().getArtifactHandler(this).publish(target);
        } finally {
            util.afterDeployArtifacts();
        }
    }

    protected void copyArtifactsToStagingDirectory() throws IOException, AzureExecutionException {
        MavenUtility.prepareResources(this.getProject(), this.getSession(), this.getMavenResourcesFiltering(),
        		this.getResources(), getDeploymentStagingDirectoryPath());
        MavenUtility.assureStagingDirectoryNotEmpty(getDeploymentStagingDirectoryPath());
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
            if (stagingDirectory != null) {
                FileUtils.forceDeleteOnExit(stagingDirectory);
            }
        }
    }
}
