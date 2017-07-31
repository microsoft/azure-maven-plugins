/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which deploy function to Azure.
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
        getArtifactHandler().publish(getResources());
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

    protected List<Resource> getResources() {
        final Resource resource = new Resource();
        resource.setDirectory(getBuildDirectoryAbsolutePath());
        resource.setTargetPath("/");
        resource.setFiltering(false);
        resource.setIncludes(Arrays.asList("*.jar"));

        final ArrayList<Resource> resources = new ArrayList<>();
        resources.add(resource);
        return resources;
    }

    protected ArtifactHandler getArtifactHandler() {
        return new FTPArtifactHandlerImpl(this);
    }
}
