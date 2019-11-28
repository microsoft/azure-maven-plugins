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
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.maven.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.maven.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.maven.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import com.microsoft.azure.maven.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.maven.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.handlers.artifact.ZIPArtifactHandlerImpl;
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

    public static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    public static final String VALID_JAVA_VERSION_PATTERN = "^1\\.8.*"; // For now we only support function with java 8

    public static final String DEPLOY_START = "Trying to deploy the function app...";
    public static final String DEPLOY_FINISH =
        "Successfully deployed the function app at https://%s.azurewebsites.net";
    public static final String FUNCTION_APP_CREATE_START = "The specified function app does not exist. " +
        "Creating a new function app...";
    public static final String FUNCTION_APP_CREATED = "Successfully created the function app: %s";
    public static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    public static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app.";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

    public static final String HOST_JAVA_VERSION = "Java version of function host : %s";
    public static final String HOST_JAVA_VERSION_OFF = "Java version of function host is not initiated," +
        " set it to Java 8";
    public static final String HOST_JAVA_VERSION_INCORRECT = "Java version of function host %s does not" +
        " meet the requirement of Azure Functions, set it to Java 8";

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
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        if (app == null) {
            createFunctionApp(runtimeHandler);
        } else {
            updateFunctionApp(app, runtimeHandler);
        }
    }

    protected void createFunctionApp(FunctionRuntimeHandler runtimeHandler) throws Exception {
        info(FUNCTION_APP_CREATE_START);
        final WithCreate withCreate = runtimeHandler.defineAppWithRuntime();
        configureAppSettings(withCreate::withAppSettings, getAppSettings());
        withCreate.withJavaVersion(DEFAULT_JAVA_VERSION).withWebContainer(null).create();
        info(String.format(FUNCTION_APP_CREATED, getAppName()));
    }

    protected void updateFunctionApp(final FunctionApp app, FunctionRuntimeHandler runtimeHandler) throws Exception {
        info(FUNCTION_APP_UPDATE);
        // Work around of https://github.com/Azure/azure-sdk-for-java/issues/1755
        app.inner().withTags(null);
        final Update update = runtimeHandler.updateAppRuntime(app);
        checkHostJavaVersion(app, update); // Check Java Version of Server
        configureAppSettings(update::withAppSettings, getAppSettings());
        update.apply();
        info(FUNCTION_APP_UPDATE_DONE + getAppName());
    }

    protected void checkHostJavaVersion(final FunctionApp app, final Update update) {
        final JavaVersion serverJavaVersion = app.javaVersion();
        if (serverJavaVersion.toString().matches(VALID_JAVA_VERSION_PATTERN)) {
            info(String.format(HOST_JAVA_VERSION, serverJavaVersion));
        } else if (serverJavaVersion.equals(JavaVersion.OFF)) {
            info(HOST_JAVA_VERSION_OFF);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        } else {
            warning(HOST_JAVA_VERSION_INCORRECT);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        }
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

    protected FunctionRuntimeHandler getFunctionRuntimeHandler() throws MojoExecutionException, AzureAuthFailureException {
        final FunctionRuntimeHandler.Builder<?> builder;
        final OperatingSystemEnum os = this.runtime == null ? RuntimeConfiguration.DEFAULT_OS : runtime.getOperationSystemEnum();
        switch (os) {
            case Windows:
                builder = new WindowsFunctionRuntimeHandler.Builder();
                break;
            case Linux:
                builder = new LinuxFunctionRuntimeHandler.Builder();
                break;
            default:
                throw new MojoExecutionException(String.format("Unsupported runtime %s", os));
        }
        return builder.appName(getAppName())
                .resourceGroup(getResourceGroup())
                .runtime(getRuntime())
                .region(Region.fromName(region))
                .pricingTier(getPricingTier())
                .servicePlanName(getAppServicePlanName())
                .servicePlanResourceGroup(getAppServicePlanResourceGroup())
                .azure(getAzureClient())
                .log(getLog())
                .build();
    }

    protected ArtifactHandler getArtifactHandler() throws MojoExecutionException {
        final ArtifactHandlerBase.Builder builder;

        switch (this.getDeploymentType()) {
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
            case EMPTY:
            case RUN_FROM_ZIP:
                builder = new RunFromZipArtifactHandlerImpl.Builder();
                break;
            default:
                throw new MojoExecutionException(
                    "The value of <deploymentType> is unknown, supported values are: ftp, zip and msdeploy.");
        }
        return builder.project(this.getProject())
            .session(this.getSession())
            .filtering(this.getMavenResourcesFiltering())
            .resources(this.getResources())
            .stagingDirectoryPath(this.getDeploymentStagingDirectoryPath())
            .buildDirectoryAbsolutePath(this.getBuildDirectoryAbsolutePath())
            .log(this.getLog())
            .build();
    }

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = super.getTelemetryProperties();

        try {
            map.put(DEPLOYMENT_TYPE_KEY, getDeploymentType().toString());
        } catch (MojoExecutionException e) {
            map.put(DEPLOYMENT_TYPE_KEY, "Unknown deployment type.");
        }
        return map;
    }

    //endregion
}
