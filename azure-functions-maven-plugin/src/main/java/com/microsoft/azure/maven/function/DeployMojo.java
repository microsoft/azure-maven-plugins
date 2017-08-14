/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.NewAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Deploy artifacts to target Function App in Azure. If target Function App doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    public static final String FUNCTION_DEPLOY_START = "Starting deploying to Function App ";
    public static final String FUNCTION_DEPLOY_SUCCESS =
            "Successfully deployed Function App at https://%s.azurewebsites.net";
    public static final String FUNCTION_APP_CREATE_START = "Target Function App does not exist. " +
            "Creating a new Function App ...";
    public static final String FUNCTION_APP_CREATED = "Successfully created Function App ";

    @Override
    protected void doExecute() throws Exception {
        getLog().info(FUNCTION_DEPLOY_START + getAppName() + "...");

        createFunctionAppIfNotExist();

        getArtifactHandler().publish();

        getLog().info(String.format(FUNCTION_DEPLOY_SUCCESS, getAppName()));
    }

    protected void createFunctionAppIfNotExist() {
        final FunctionApp app = getFunctionApp();

        if (app == null) {
            getLog().info(FUNCTION_APP_CREATE_START);

            Stream.of(getAppName())
                    .map(this::defineApp)
                    .map(this::configureRegion)
                    .map(this::configureResourceGroup)
                    .map(this::configurePricingTier)
                    .map(this::configureAppSettings)
                    .forEach(w -> w.create());

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
    }

    protected WithCreate configureAppSettings(final WithCreate withCreate) {
        final Map appSettings = getAppSettings();
        if (appSettings != null && !appSettings.isEmpty()) {
            withCreate.withAppSettings(appSettings);
        }
        return withCreate;
    }

    protected ArtifactHandler getArtifactHandler() {
        if (StringUtils.isEmpty(getDeploymentType())) {
            return new MSDeployArtifactHandlerImpl(this);
        }

        switch (getDeploymentType().toLowerCase(Locale.ENGLISH)) {
            case "ftp":
                return new FTPArtifactHandlerImpl(this);
            case "msdeploy":
            default:
                return new MSDeployArtifactHandlerImpl(this);
        }
    }
}
