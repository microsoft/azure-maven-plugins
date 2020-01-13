/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.maven.function.handlers.artifact.DockerArtifactHandler;
import com.microsoft.azure.maven.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.runtime.DockerFunctionRuntimeHandler;
import com.microsoft.azure.maven.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.maven.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.maven.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.maven.handlers.ArtifactHandler;
import com.microsoft.azure.maven.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.maven.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.utils.AppServiceUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Map;
import java.util.function.Consumer;

import static com.microsoft.azure.maven.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.maven.appservice.DeploymentType.EMPTY;
import static com.microsoft.azure.maven.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.maven.appservice.DeploymentType.RUN_FROM_ZIP;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {

    public static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    public static final String VALID_JAVA_VERSION_PATTERN = "^1\\.8.*"; // For now we only support function with java 8

    public static final String DEPLOY_START = "Trying to deploy the function app...";
    public static final String DEPLOY_FINISH =
        "Successfully deployed the function app at https://%s.azurewebsites.net.";
    public static final String FUNCTION_APP_CREATE_START = "The specified function app does not exist. " +
        "Creating a new function app...";
    public static final String FUNCTION_APP_CREATED = "Successfully created the function app: %s.";
    public static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    public static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app: %s.";
    public static final String DEPLOYMENT_TYPE_KEY = "deploymentType";

    public static final String HOST_JAVA_VERSION = "Java version of function host : %s";
    public static final String HOST_JAVA_VERSION_OFF = "Java version of function host is not initiated," +
        " set it to Java 8.";
    public static final String HOST_JAVA_VERSION_INCORRECT = "Java version of function host %s does not" +
        " meet the requirement of Azure Functions, set it to Java 8.";
    public static final String UNKNOW_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown, supported values are: " +
            "ftp, zip, msdeploy, run_from_blob and run_from_zip.";

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
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        final WithCreate withCreate = runtimeHandler.defineAppWithRuntime();
        configureAppSettings(withCreate::withAppSettings, getAppSettingsWithDefaultValue());
        withCreate.withJavaVersion(DEFAULT_JAVA_VERSION).withWebContainer(null).create();
        Log.info(String.format(FUNCTION_APP_CREATED, getAppName()));
    }

    protected void updateFunctionApp(final FunctionApp app) throws AzureAuthFailureException, AzureExecutionException {
        Log.info(FUNCTION_APP_UPDATE);
        // Work around of https://github.com/Azure/azure-sdk-for-java/issues/1755
        app.inner().withTags(null);
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        runtimeHandler.updateAppServicePlan(app);
        final Update update = runtimeHandler.updateAppRuntime(app);
        checkHostJavaVersion(app, update); // Check Java Version of Server
        configureAppSettings(update::withAppSettings, getAppSettingsWithDefaultValue());
        update.apply();
        Log.info(String.format(FUNCTION_APP_UPDATE_DONE, getAppName()));
    }

    protected void checkHostJavaVersion(final FunctionApp app, final Update update) {
        final JavaVersion serverJavaVersion = app.javaVersion();
        if (serverJavaVersion.toString().matches(VALID_JAVA_VERSION_PATTERN)) {
            Log.info(String.format(HOST_JAVA_VERSION, serverJavaVersion));
        } else if (serverJavaVersion.equals(JavaVersion.OFF)) {
            Log.info(HOST_JAVA_VERSION_OFF);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        } else {
            Log.warn(HOST_JAVA_VERSION_INCORRECT);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        }
    }

    protected void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
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
                        .serverId(runtime.getServerId())
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
                .mavenSettings(getSettings())
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
                throw new AzureExecutionException(UNKNOW_DEPLOYMENT_TYPE);
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
