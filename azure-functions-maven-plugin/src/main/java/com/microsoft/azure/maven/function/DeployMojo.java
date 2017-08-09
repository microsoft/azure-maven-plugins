/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.*;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * Deploy artifacts to target Function App in Azure. If target Function App doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    public static final String FUNCTION_DEPLOY_START = "Starting deploying to Function App ";
    public static final String FUNCTION_DEPLOY_SUCCESS = "Successfully deployed to Function App ";
    public static final String FUNCTION_APP_CREATE_START = "Target Function App does not exist. " +
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

            Arrays.asList(getAppName()).stream()
                    .map(this::defineApp)
                    .map(this::configureRegion)
                    .map(this::configureResourceGroup)
                    .map(this::configurePricingTier)
                    .map(this::configureAppSettings)
                    .map(w -> w.create());

            getLog().info(FUNCTION_APP_CREATED + getAppName());
        }
    }

    protected Blank defineApp(final String appName) {
        return getAzureClient().appServices().functionApps().define(appName);
    }

    protected NewAppServicePlanWithGroup configureRegion(final Blank blank) {
        return blank.withRegion(getRegion());
    }

    protected WithCreate configureResourceGroup(final NewAppServicePlanWithGroup n) {
        final String group = getResourceGroup();
        return isResourceGroupExist(group) ?
                n.withExistingResourceGroup(group) :
                n.withNewResourceGroup(group);
    }

    protected boolean isResourceGroupExist(final String resourceGroup) {
        return getAzureClient().resourceGroups().checkExistence(resourceGroup);
    }

    protected WithCreate configurePricingTier(final WithCreate withCreate) {
        if (getPricingTier() != null) {
            withCreate.withNewAppServicePlan(getPricingTier());
        } else {
            withCreate.withNewConsumptionPlan();
        }
        return withCreate;
    };

    protected WithCreate configureAppSettings(final WithCreate withCreate) {
        final Map appSettings = getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            withCreate.withAppSettings(appSettings);
        }
        return withCreate;
    };

    protected ArtifactHandler getArtifactHandler() {
        return new FTPArtifactHandlerImpl(this);
    }
}
