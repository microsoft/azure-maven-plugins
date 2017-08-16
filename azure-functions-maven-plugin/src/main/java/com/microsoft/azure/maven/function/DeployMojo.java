/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.NewAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

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
    public static final String FUNCTION_APP_UPDATE = "Updating Function App...";
    public static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated Function App ";

    public static final String MS_DEPLOY = "msdeploy";
    public static final String FTP = "ftp";
    public static final String WEST_US = "westus";

    /**
     * Function App region, which will only be used to create Function App at the first time.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.region", defaultValue = WEST_US)
    protected String region;

    /**
     * Function App pricing tier, which will only be used to create Function App at the first time.<br/>
     * Below is the list of supported pricing tier. If left blank, Consumption plan is the default.
     * <ul>
     * <li>F1</li>
     * <li>D1</li>
     * <li>B1</li>
     * <li>B2</li>
     * <li>B3</li>
     * <li>S1</li>
     * <li>S2</li>
     * <li>S3</li>
     * <li>P1</li>
     * <li>P2</li>
     * <li>P3</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.pricingTier")
    protected PricingTierEnum pricingTier;

    /**
     * Deployment type to deploy Web App. Supported values:
     * <ul>
     * <li>msdeploy</li>
     * <li>ftp</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.deploymentType", defaultValue = MS_DEPLOY)
    protected String deploymentType;

    public String getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier == null ? null : pricingTier.toPricingTier();
    }

    public String getDeploymentType() {
        return StringUtils.isEmpty(deploymentType) ? MS_DEPLOY : deploymentType;
    }

    @Override
    protected void doExecute() throws Exception {
        getLog().info(FUNCTION_DEPLOY_START + getAppName() + "...");

        createOrUpdateFunctionApp();

        getArtifactHandler().publish();

        getLog().info(String.format(FUNCTION_DEPLOY_SUCCESS, getAppName()));
    }

    protected void createOrUpdateFunctionApp() throws Exception {
        final FunctionApp app = getFunctionApp();
        if (app == null) {
            createFunctionApp();
        } else {
            updateFunctionApp(app);
        }
    }

    protected void createFunctionApp() throws Exception {
        getLog().info(FUNCTION_APP_CREATE_START);

        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = defineApp(getAppName(), getRegion());
        final WithCreate withCreate = configureResourceGroup(newAppServicePlanWithGroup, getResourceGroup());
        configurePricingTier(withCreate, getPricingTier());
        configureAppSettings(withCreate::withAppSettings, getAppSettings());
        withCreate.create();

        getLog().info(FUNCTION_APP_CREATED + getAppName());
    }

    protected void updateFunctionApp(final FunctionApp app) {
        getLog().info(FUNCTION_APP_UPDATE);

        final Update update = app.update();
        configureAppSettings(update::withAppSettings, getAppSettings());
        update.apply();

        getLog().info(FUNCTION_APP_UPDATE_DONE + getAppName());
    }

    protected NewAppServicePlanWithGroup defineApp(final String appName, final String region) throws Exception {
        return getAzureClient().appServices().functionApps().define(appName).withRegion(region);
    }

    protected WithCreate configureResourceGroup(final NewAppServicePlanWithGroup newAppServicePlanWithGroup,
                                                final String resourceGroup) throws Exception {
        return isResourceGroupExist(resourceGroup) ?
                newAppServicePlanWithGroup.withExistingResourceGroup(resourceGroup) :
                newAppServicePlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    protected boolean isResourceGroupExist(final String resourceGroup) throws Exception {
        return getAzureClient().resourceGroups().checkExistence(resourceGroup);
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

    protected ArtifactHandler getArtifactHandler() {
        switch (getDeploymentType().toLowerCase(Locale.ENGLISH)) {
            case FTP:
                return new FTPArtifactHandlerImpl(this);
            case MS_DEPLOY:
            default:
                return new MSDeployArtifactHandlerImpl(this);
        }
    }
}
