/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.Utils;
import com.microsoft.azure.common.applicationinsights.ApplicationInsightsManager;
import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.handlers.artifact.DockerArtifactHandler;
import com.microsoft.azure.common.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.runtime.DockerFunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.common.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;
import java.util.function.Consumer;

import static com.microsoft.azure.common.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.common.appservice.DeploymentType.EMPTY;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_ZIP;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {

    private static final String DEPLOY_START = "Trying to deploy the function app...";
    private static final String DEPLOY_FINISH =
        "Successfully deployed the function app at https://%s.azurewebsites.net.";
    private static final String FUNCTION_APP_CREATE_START = "The specified function app does not exist. " +
        "Creating a new function app...";
    private static final String FUNCTION_APP_CREATED = "Successfully created the function app: %s.";
    private static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    private static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app: %s.";
    private static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
    private static final String UNKNOWN_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown, supported values are: " +
            "ftp, zip, msdeploy, run_from_blob and run_from_zip.";
    private static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHTS_NOT_SUPPORTED = "Application Insights features are not supported with" +
            " current authentication, skip related steps.";
    private static final String APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT = "Contradictory configurations for application insights," +
            " specify 'appInsightsKey' or 'appInsightsInstance' if you want to enable it, and specify " +
            "'disableAppInsights=true' if you want to disable it.";
    private static final String FAILED_TO_GET_APPLICATION_INSIGHTS = "The application insights %s cannot be found, " +
            "will create it in resource group %s.";
    private static final String SKIP_CREATING_APPLICATION_INSIGHTS = "Skip creating application insights";
    private static final String APPLICATION_INSIGHTS_CREATE_START = "Creating application insights...";
    private static final String APPLICATION_INSIGHTS_CREATED = "Successfully created the application insights %s " +
            "for this Function App. You can visit https://portal.azure.com/#resource%s/overview to view your " +
            "Application Insights component.";
    private static final String APPLICATION_INSIGHTS_CREATE_FAILED = "Unable to create the Application Insights " +
            "for the Function App due to error %s. Please use the Azure Portal to manually create and configure the " +
            "Application Insights if needed.";
    private static final String INSTRUMENTATION_KEY_IS_NOT_VALID = "Instrumentation key is not valid, " +
            "please update the application insights configuration";

    //region Entry Point
    @Override
    protected void doExecute() throws AzureExecutionException {
        try {
            createOrUpdateFunctionApp();

            final FunctionApp app = getFunctionApp();
            if (app == null) {
                throw new AzureExecutionException(
                    String.format("Failed to get the function app with name: %s", getAppName()));
            }

            final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);

            Log.info(DEPLOY_START);

            getArtifactHandler().publish(deployTarget);

            Log.info(String.format(DEPLOY_FINISH, getAppName()));
        } catch (AzureAuthFailureException e) {
            throw new AzureExecutionException("Cannot auth to azure", e);
        }
    }

    //endregion

    //region Create or update Azure Functions

    protected void createOrUpdateFunctionApp() throws AzureAuthFailureException, AzureExecutionException {
        final FunctionApp app = getFunctionApp();
        if (app == null) {
            createFunctionApp();
        } else {
            updateFunctionApp(app);
        }
    }

    protected void createFunctionApp() throws AzureAuthFailureException, AzureExecutionException {
        Log.info(FUNCTION_APP_CREATE_START);
        validateApplicationInsightsConfiguration();
        final Map appSettings = getAppSettingsWithDefaultValue();
        // get/create ai instances only if user didn't specify ai connection string in app settings
        bindApplicationInsights(appSettings, true);
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        final WithCreate withCreate = runtimeHandler.defineAppWithRuntime();
        withCreate.withAppSettings(appSettings).create();
        Log.info(String.format(FUNCTION_APP_CREATED, getAppName()));
    }

    protected void updateFunctionApp(final FunctionApp app) throws AzureAuthFailureException, AzureExecutionException {
        Log.info(FUNCTION_APP_UPDATE);
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        runtimeHandler.updateAppServicePlan(app);
        final Update update = runtimeHandler.updateAppRuntime(app);
        validateApplicationInsightsConfiguration();
        final Map appSettings = getAppSettingsWithDefaultValue();
        if (isDisableAppInsights()) {
            // Remove App Insights connection when `disableAppInsights` set to true
            // Need to call `withoutAppSetting` as withAppSettings will only not remove parameters
            update.withoutAppSetting(APPINSIGHTS_INSTRUMENTATION_KEY);
        } else {
            bindApplicationInsights(appSettings, false);
        }
        configureAppSettings(update::withAppSettings, appSettings);
        update.apply();
        Log.info(String.format(FUNCTION_APP_UPDATE_DONE, getAppName()));
    }

    protected void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    /**
     * Binding Function App with Application Insights
     * Will follow the below sequence appInsightsKey -> appInsightsInstance -> Create New AI Instance (Function creation only)
     * @param appSettings App settings map
     * @param isCreation Define the stage of function app, as we only create ai instance by default when create new function apps
     * @throws AzureExecutionException When there are conflicts in configuration or meet errors while finding/creating application insights instance
     */
    private void bindApplicationInsights(Map appSettings, boolean isCreation) throws AzureExecutionException, AzureAuthFailureException {
        // Skip app insights creation when user specify ai connection string in app settings
        if (appSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY)) {
            return;
        }
        String instrumentationKey = null;
        if (StringUtils.isNotEmpty(getAppInsightsKey())) {
            instrumentationKey = getAppInsightsKey();
            if (!Utils.isGUID(instrumentationKey)) {
                throw new AzureExecutionException(INSTRUMENTATION_KEY_IS_NOT_VALID);
            }
        } else {
            final ApplicationInsightsComponent applicationInsightsComponent = getOrCreateApplicationInsights(isCreation);
            instrumentationKey = applicationInsightsComponent == null ? null : applicationInsightsComponent.instrumentationKey();
        }
        if (StringUtils.isNotEmpty(instrumentationKey)) {
            appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
        }
    }

    private void validateApplicationInsightsConfiguration() throws AzureExecutionException {
        if (isDisableAppInsights() && (StringUtils.isNotEmpty(getAppInsightsKey()) || StringUtils.isNotEmpty(getAppInsightsInstance()))) {
            throw new AzureExecutionException(APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT);
        }
    }

    private ApplicationInsightsComponent getOrCreateApplicationInsights(boolean enableCreation) throws AzureAuthFailureException {
        final AzureTokenCredentials credentials = getAzureTokenWrapper();
        if (credentials == null) {
            Log.warn(APPLICATION_INSIGHTS_NOT_SUPPORTED);
            return null;
        }
        final String subscriptionId = getAzureClient().subscriptionId();
        final ApplicationInsightsManager applicationInsightsManager = new ApplicationInsightsManager(credentials, subscriptionId, getUserAgent());
        return StringUtils.isNotEmpty(getAppInsightsInstance()) ?
                getApplicationInsights(applicationInsightsManager, getAppInsightsInstance()) :
                enableCreation ? createApplicationInsights(applicationInsightsManager, getAppName()) : null;
    }

    private ApplicationInsightsComponent getApplicationInsights(ApplicationInsightsManager applicationInsightsManager,
                                                                String appInsightsInstance) {
        final ApplicationInsightsComponent resource = applicationInsightsManager.getApplicationInsightsInstance(getResourceGroup(),
                appInsightsInstance);
        if (resource == null) {
            Log.warn(String.format(FAILED_TO_GET_APPLICATION_INSIGHTS, appInsightsInstance, getResourceGroup()));
            return createApplicationInsights(applicationInsightsManager, appInsightsInstance);
        }
        return resource;
    }

    private ApplicationInsightsComponent createApplicationInsights(ApplicationInsightsManager applicationInsightsManager, String name) {
        if (isDisableAppInsights()) {
            Log.info(SKIP_CREATING_APPLICATION_INSIGHTS);
            return null;
        }
        try {
            Log.info(APPLICATION_INSIGHTS_CREATE_START);
            final ApplicationInsightsComponent resource = applicationInsightsManager.createApplicationInsights(getResourceGroup(), name, getRegion());
            Log.info(String.format(APPLICATION_INSIGHTS_CREATED, resource.name(), resource.id()));
            return resource;
        } catch (Exception e) {
            Log.warn(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, e.getMessage()));
            return null;
        }
    }

    //endregion

    protected FunctionRuntimeHandler getFunctionRuntimeHandler() throws AzureAuthFailureException, AzureExecutionException {
        final FunctionRuntimeHandler.Builder<?> builder;
        final OperatingSystemEnum os = getOsEnum();
        switch (os) {
            case Windows:
                builder = new WindowsFunctionRuntimeHandler.Builder();
                break;
            case Linux:
                builder = new LinuxFunctionRuntimeHandler.Builder();
                break;
            case Docker:
                final RuntimeConfiguration runtime = this.getRuntime();
                builder = new DockerFunctionRuntimeHandler.Builder()
                        .image(runtime.getImage())
                        .dockerCredentialProvider(MavenDockerCredentialProvider.fromMavenSettings(this.getSettings(), runtime.getServerId()))
                        .registryUrl(runtime.getRegistryUrl());
                break;
            default:
                throw new AzureExecutionException(String.format("Unsupported runtime %s", os));
        }
        return builder.appName(getAppName())
                .resourceGroup(getResourceGroup())
                .runtime(getRuntime())
                .region(Region.fromName(region))
                .pricingTier(getPricingTier())
                .servicePlanName(getAppServicePlanName())
                .servicePlanResourceGroup(getAppServicePlanResourceGroup())
                .functionExtensionVersion(getFunctionExtensionVersion())
                .azure(getAzureClient())
                .build();
    }

    protected OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final String os = runtime == null ? null : runtime.getOs();
        return StringUtils.isEmpty(os) ? RuntimeConfiguration.DEFAULT_OS : Utils.parseOperationSystem(os);
    }

    protected ArtifactHandler getArtifactHandler() throws AzureExecutionException {
        final ArtifactHandlerBase.Builder builder;

        final DeploymentType deploymentType = getDeploymentType();
        switch (deploymentType) {
            case MSDEPLOY:
                builder = new MSDeployArtifactHandlerImpl.Builder().functionAppName(this.getAppName());
                break;
            case FTP:
                builder = new FTPArtifactHandlerImpl.Builder();
                break;
            case ZIP:
                builder = new ZIPArtifactHandlerImpl.Builder();
                break;
            case RUN_FROM_BLOB:
                builder = new RunFromBlobArtifactHandlerImpl.Builder();
                break;
            case DOCKER:
                builder = new DockerArtifactHandler.Builder();
                break;
            case EMPTY:
            case RUN_FROM_ZIP:
                builder = new RunFromZipArtifactHandlerImpl.Builder();
                break;
            default:
                throw new AzureExecutionException(UNKNOWN_DEPLOYMENT_TYPE);
        }
        return builder.project(ProjectUtils.convertCommonProject(this.getProject()))
            .stagingDirectoryPath(this.getDeploymentStagingDirectoryPath())
            .buildDirectoryAbsolutePath(this.getBuildDirectoryAbsolutePath())
            .build();
    }

    @Override
    public DeploymentType getDeploymentType() throws AzureExecutionException {
        final DeploymentType deploymentType = super.getDeploymentType();
        return deploymentType == EMPTY ? getDeploymentTypeByRuntime() : deploymentType;
    }

    public DeploymentType getDeploymentTypeByRuntime() throws AzureExecutionException {
        final OperatingSystemEnum operatingSystemEnum = getOsEnum();
        switch (operatingSystemEnum) {
            case Docker:
                return DOCKER;
            case Linux:
                return isDedicatedPricingTier() ? RUN_FROM_ZIP : RUN_FROM_BLOB;
            default:
                return RUN_FROM_ZIP;
        }
    }

    protected boolean isDedicatedPricingTier() {
        return AppServiceUtils.getPricingTierFromString(pricingTier) != null;
    }

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = super.getTelemetryProperties();

        try {
            map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        } catch (AzureExecutionException e) {
            map.put(DEPLOYMENT_TYPE_KEY, "Unknown deployment type.");
        }
        return map;
    }

    //endregion
}
