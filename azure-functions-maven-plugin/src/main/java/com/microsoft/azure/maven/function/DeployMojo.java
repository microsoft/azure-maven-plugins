/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Goal which deploy artifacts to specified Function App in Azure.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    public static final String FUNCTION_DEPLOY_START = "Starting deploying to Function App ";
    public static final String FUNCTION_DEPLOY_SUCCESS = "Successfully deployed to Function App ";
    public static final String FUNCTION_APP_CREATE_START = "Specified Function App does not exist. " +
            "Creating a new Function App ...";
    public static final String FUNCTION_APP_CREATED = "Successfully created Function App ";

    @Override
    protected void doExecute() throws Exception {
        getLog().info(FUNCTION_DEPLOY_START + getAppName() + "...");

        createFunctionAppIfNotExist();
        getArtifactHandler().publish();
        getFunctionApp().syncTriggers();

        getLog().info(FUNCTION_DEPLOY_SUCCESS + getAppName());
    }

    protected void createFunctionAppIfNotExist() {
        final FunctionApp app = getFunctionApp();

        if (app == null) {
            getLog().info(FUNCTION_APP_CREATE_START);
            getAzureClient().appServices().functionApps()
                    .define(getAppName())
                    .withRegion(getRegion())
                    .withNewResourceGroup(getResourceGroup())
                    .create();
            getLog().info(FUNCTION_APP_CREATED + getAppName());
        }
    }

    protected ArtifactHandler getArtifactHandler() {
        return new FTPArtifactHandlerImpl(this);
    }
}
