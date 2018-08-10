/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.ExistingAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.NewAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    public static final String DEPLOY_START = "Trying to deploy the function app...";
    public static final String DEPLOY_FINISH =
        "Successfully deployed the function app at https://%s.azurewebsites.net.";
    public static final String FUNCTION_APP_CREATE_START = "The specified function app does not exist. " +
        "Creating a new function app...";
    public static final String FUNCTION_APP_CREATED = "Successfully created a function app.";
    public static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    public static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app.";

    //region Entry Point

    @Override
    protected void doExecute() throws Exception {
        createOrUpdateFunctionApp();

        final FunctionApp app = getFunctionApp();
        if (app == null) {
            throw new MojoExecutionException(
                String.format("Failed to get the function app with name: %s", getAppName()));
        }

        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);

        info(DEPLOY_START);

        getArtifactHandler().publish(deployTarget);

        info(String.format(DEPLOY_FINISH, getAppName()));
    }

    //endregion

    //region Create or update Azure Functions

    protected void createOrUpdateFunctionApp() throws Exception {
        final FunctionApp app = getFunctionApp();
        if (app == null) {
            createFunctionApp();
        } else {
            updateFunctionApp(app);
        }
    }

    protected void createFunctionApp() throws Exception {
        info(FUNCTION_APP_CREATE_START);

        final AppServicePlan plan = AppServiceUtils.getAppServicePlan(this);
        final Blank functionApp = getAzureClient().appServices().functionApps().define(appName);
        final String resGrp = getResourceGroup();
        final WithCreate withCreate;
        if (plan == null) {
            final NewAppServicePlanWithGroup newAppServicePlanWithGroup = functionApp.withRegion(region);
            withCreate = configureResourceGroup(newAppServicePlanWithGroup, resGrp);
            configurePricingTier(withCreate, getPricingTier());
        } else {
            final ExistingAppServicePlanWithGroup planWithGroup = functionApp.withExistingAppServicePlan(plan);
            withCreate = isResourceGroupExist(resGrp) ?
                    planWithGroup.withExistingResourceGroup(resGrp) :
                    planWithGroup.withNewResourceGroup(resGrp);
        }
        configureAppSettings(withCreate::withAppSettings, getAppSettings());
        withCreate.create();

        info(FUNCTION_APP_CREATED + getAppName());
    }

    protected void updateFunctionApp(final FunctionApp app) {
        info(FUNCTION_APP_UPDATE);

        // Work around of https://github.com/Azure/azure-sdk-for-java/issues/1755
        app.inner().withTags(null);

        final Update update = app.update();
        configureAppSettings(update::withAppSettings, getAppSettings());
        update.apply();

        info(FUNCTION_APP_UPDATE_DONE + getAppName());
    }

    protected WithCreate configureResourceGroup(final NewAppServicePlanWithGroup newAppServicePlanWithGroup,
                                                final String resourceGroup) throws Exception {
        return isResourceGroupExist(resourceGroup) ?
                newAppServicePlanWithGroup.withExistingResourceGroup(resourceGroup) :
                newAppServicePlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    protected boolean isResourceGroupExist(final String resourceGroup) throws Exception {
        return getAzureClient().resourceGroups().contain(resourceGroup);
    }

    protected void configurePricingTier(final WithCreate withCreate, final PricingTier pricingTier) {
        if (pricingTier != null) {
            // Enable Always On when using app service plan
            withCreate.withNewAppServicePlan(pricingTier).withWebAppAlwaysOn(true);
        } else {
            withCreate.withNewConsumptionPlan();
        }
    }

    protected void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    //endregion

    protected ArtifactHandler getArtifactHandler() throws MojoExecutionException {
        switch (this.getDeploymentType()) {
            case MSDEPLOY:
                return new MSDeployArtifactHandlerImpl(this);
            case FTP:
                return new FTPArtifactHandlerImpl(this);
            case EMPTY:
            case ZIP:
                return new ZIPArtifactHandlerImpl(this);
            default:
                throw new MojoExecutionException(
                    "The value of <deploymentType> is unknown, supported values are: ftp, zip and msdeploy.");
        }
    }
}
