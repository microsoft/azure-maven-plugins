/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    public static final String WEBAPP_DEPLOY_START = "Start deploying to Web App %s...";
    public static final String WEBAPP_DEPLOY_SUCCESS = "Successfully deployed Web App at https://%s.azurewebsites.net";
    public static final String WEBAPP_NOT_EXIST = "Target Web App doesn't exist. Creating a new one...";
    public static final String WEBAPP_CREATED = "Successfully created Web App.";
    public static final String UPDATE_WEBAPP = "Updating target Web App...";
    public static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App.";
    public static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    public static final String START_APP = "Starting Web App after deploying artifacts...";
    public static final String STOP_APP_DONE = "Successfully stopped Web App.";
    public static final String START_APP_DONE = "Successfully started Web App.";

    protected DeploymentUtil util = new DeploymentUtil();

    @Override
    protected void doExecute() throws Exception {
        getLog().info(String.format(WEBAPP_DEPLOY_START, getAppName()));

        createOrUpdateWebApp();
        deployArtifacts();

        getLog().info(String.format(WEBAPP_DEPLOY_SUCCESS, getAppName()));
    }

    protected void createOrUpdateWebApp() throws Exception {
        final WebApp app = getWebApp();
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
    }

    protected void deployArtifacts() throws Exception {
        try {
            util.beforeDeployArtifacts();
            getFactory().getArtifactHandler(this).publish();
        } finally {
            util.afterDeployArtifacts();
        }
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
