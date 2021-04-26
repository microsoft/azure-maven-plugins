/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.common.Utils;
import com.microsoft.azure.common.applicationinsights.ApplicationInsightsManager;
import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.handlers.artifact.DockerArtifactHandler;
import com.microsoft.azure.common.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.runtime.DockerFunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.common.function.model.FunctionResource;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.common.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.ProjectUtils;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.microsoft.azure.common.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.common.appservice.DeploymentType.EMPTY;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_ZIP;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {

    private static final int LIST_TRIGGERS_MAX_RETRY = 3;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH =
            "Deployment done, you may access your resource through %s";
    private static final String FUNCTION_APP_CREATE_START = "The specified function app does not exist. " +
            "Creating a new function app...";
    private static final String FUNCTION_APP_CREATED = "Successfully created the function app: %s.";
    private static final String FUNCTION_SLOT_CREATE_START = "The specified function slot does not exist. " +
            "Creating a new slot...";
    private static final String FUNCTION_SLOT_CREATED = "Successfully created the function slot: %s.";
    private static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    private static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app: %s.";
    private static final String FUNCTION_SLOT_UPDATE = "Updating the specified function slot...";
    private static final String FUNCTION_SLOT_UPDATE_DONE = "Successfully updated the function slot: %s.";
    private static final String DEPLOYMENT_TYPE_KEY = "deploymentType";
    private static final String UNKNOWN_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown, supported values are: " +
            "ftp, zip, msdeploy, run_from_blob and run_from_zip.";
    private static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT = "Contradictory configurations for application insights," +
            " specify 'appInsightsKey' or 'appInsightsInstance' if you want to enable it, and specify " +
            "'disableAppInsights=true' if you want to disable it.";
    private static final String FAILED_TO_GET_APPLICATION_INSIGHTS = "The application insights %s cannot be found, " +
            "will create it in resource group %s.";
    private static final String SKIP_CREATING_APPLICATION_INSIGHTS = "Skip creating application insights";
    private static final String APPLICATION_INSIGHTS_CREATE_START = "Creating application insights...";
    private static final String APPLICATION_INSIGHTS_CREATED = "Successfully created the application insights %s " +
            "for this Function App. You can visit %s/#@/resource%s/overview to view your " +
            "Application Insights component.";
    private static final String APPLICATION_INSIGHTS_CREATE_FAILED = "Unable to create the Application Insights " +
            "for the Function App due to error %s. Please use the Azure Portal to manually create and configure the " +
            "Application Insights if needed.";
    private static final String INSTRUMENTATION_KEY_IS_NOT_VALID = "Instrumentation key is not valid, " +
            "please update the application insights configuration";
    private static final String FAILED_TO_GET_FUNCTION_APP_PRICING_TIER = "Failed to get function app pricing tier";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer https://aka.ms/azure-functions-key.";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app, " +
            "please try recompile the project by `mvn clean package` and deploy again.";
    private static final String SYNCING_TRIGGERS_AND_FETCH_FUNCTION_INFORMATION = "Syncing triggers and fetching function information (Attempt %d/%d)...";
    private static final String ARTIFACT_INCOMPATIBLE = "Your function app artifact compile version is higher than the java version in function host, " +
            "please downgrade the project compile version and try again.";
    private static final String FUNCTION_APP_NOT_EXISTS = "Cannot find the Function App '%s' when creating deployment slot.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java.";
    private static final String CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING = "App setting `FUNCTIONS_WORKER_RUNTIME` doesn't " +
            "meet the requirement of Azure Java Functions, the value should be `java`.";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
            "isn't configured, setting up the default value.";
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    private static final String RUNNING = "Running";
    private static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    private static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9._\\-()]{1,90}";
    private static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private static final String EMPTY_APP_NAME = "Please config the <appName> in pom.xml.";
    private static final String INVALID_APP_NAME = "The <appName> only allow alphanumeric characters, hyphens and cannot start or end in a hyphen.";
    private static final String EMPTY_RESOURCE_GROUP = "Please config the <resourceGroup> in pom.xml.";
    private static final String INVALID_RESOURCE_GROUP_NAME = "The <resourceGroup> only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final String INVALID_SERVICE_PLAN_NAME = "Invalid value for <appServicePlanName>, it need to match the pattern %s";
    private static final String INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME = "Invalid value for <appServicePlanResourceGroup>, it only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final String EMPTY_SLOT_NAME = "Please config the <name> of <deploymentSlot> in pom.xml";
    private static final String INVALID_SLOT_NAME = "Invalid value of <name> inside <deploymentSlot> in pom.xml, it needs to match the pattern '%s'";
    private static final String INVALID_REGION = "The value of <region> is not supported, please correct it in pom.xml.";
    private static final String EMPTY_IMAGE_NAME = "Please config the <image> of <runtime> in pom.xml.";

    private JavaVersion parsedJavaVersion;

    @Override
    public DeploymentType getDeploymentType() throws AzureExecutionException {
        final DeploymentType deploymentType = super.getDeploymentType();
        return deploymentType == EMPTY ? getDeploymentTypeByRuntime() : deploymentType;
    }

    @Override
    protected void doExecute() throws AzureExecutionException {
        try {
            validateParameters();

            parseConfiguration();

            checkArtifactCompileVersion();

            final WebAppBase target = createOrUpdateResource();

            if (target == null) {
                throw new AzureExecutionException(
                        String.format("Failed to get the deploy target with name: %s", getAppName()));
            }

            deployArtifact(new DeployTarget(target, DeployTargetType.FUNCTION));

            if (!isDeployToSlot()) {
                listHTTPTriggerUrls();
            }
        } catch (AzureAuthFailureException e) {
            throw new AzureExecutionException("Cannot auth to azure", e);
        }
    }

    private void deployArtifact(DeployTarget deployTarget) throws AzureExecutionException, AzureAuthFailureException {
        Log.info(DEPLOY_START);
        final ArtifactHandler artifactHandler = getArtifactHandler();
        executeWithTimeRecorder(() -> artifactHandler.publish(deployTarget), DEPLOY);
        final WebAppBase target = deployTarget.getApp();
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            // start function app after deployment
            target.start();
        }
        Log.info(String.format(DEPLOY_FINISH, getResourcePortalUrl(deployTarget.getApp())));
    }

    protected WebAppBase createOrUpdateResource() throws AzureExecutionException, AzureAuthFailureException {
        final FunctionApp app = getFunctionApp();
        final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
        if (isDeployToSlot()) {
            if (app == null) {
                throw new AzureExecutionException(String.format(FUNCTION_APP_NOT_EXISTS, getAppName()));
            }
            final FunctionDeploymentSlot deploymentSlot = FunctionUtils.getFunctionDeploymentSlotByName(app, getDeploymentSlotSetting().getName());
            return deploymentSlot == null ? createDeploymentSlot(app, runtimeHandler) : updateDeploymentSlot(deploymentSlot, runtimeHandler);
        } else {
            return app == null ? createFunctionApp(runtimeHandler) : updateFunctionApp(app, runtimeHandler);
        }
    }

    protected FunctionApp createFunctionApp(final FunctionRuntimeHandler runtimeHandler) throws AzureAuthFailureException, AzureExecutionException {
        Validate.isTrue(StringUtils.isNotEmpty(region), "<region> is required when creating new resource");
        Log.info(FUNCTION_APP_CREATE_START);
        getTelemetryProxy().addDefaultProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
        validateApplicationInsightsConfiguration();
        final Map appSettings = getAppSettings();
        // get/create ai instances only if user didn't specify ai connection string in app settings
        bindApplicationInsights(appSettings, true);
        final WithCreate withCreate = runtimeHandler.defineAppWithRuntime();
        final FunctionApp result = (FunctionApp) withCreate.withAppSettings(appSettings).create();
        Log.info(String.format(FUNCTION_APP_CREATED, getAppName()));
        return result;
    }

    protected FunctionApp updateFunctionApp(final FunctionApp app, final FunctionRuntimeHandler runtimeHandler)
            throws AzureAuthFailureException, AzureExecutionException {
        Log.info(FUNCTION_APP_UPDATE);
        runtimeHandler.updateAppServicePlan(app);
        final Update update = runtimeHandler.updateAppRuntime(app);
        updateFunctionAppSettings(update);
        final FunctionApp result = update.apply();
        Log.info(String.format(FUNCTION_APP_UPDATE_DONE, getAppName()));
        return result;
    }

    protected FunctionDeploymentSlot createDeploymentSlot(final FunctionApp functionApp, final FunctionRuntimeHandler runtimeHandler)
            throws AzureExecutionException, AzureAuthFailureException {
        Log.info(FUNCTION_SLOT_CREATE_START);
        final DeploymentSlotSetting slotSetting = getDeploymentSlotSetting();
        final FunctionDeploymentSlot.DefinitionStages.WithCreate withCreate = runtimeHandler.createDeploymentSlot(functionApp, slotSetting);
        // Call update after creation for app settings as we can't modify the app settings from creation source during deployment creation
        final FunctionDeploymentSlot result = withCreate.create();
        Log.info(String.format(FUNCTION_SLOT_CREATED, result.name()));
        return updateDeploymentSlot(withCreate.create(), runtimeHandler);
    }

    protected FunctionDeploymentSlot updateDeploymentSlot(final FunctionDeploymentSlot deploymentSlot, final FunctionRuntimeHandler runtimeHandler)
            throws AzureAuthFailureException, AzureExecutionException {
        Log.info(FUNCTION_SLOT_UPDATE);
        final WebAppBase.Update<FunctionDeploymentSlot> update = runtimeHandler.updateDeploymentSlot(deploymentSlot);
        updateFunctionAppSettings(update);
        final FunctionDeploymentSlot result = update.apply();
        Log.info(String.format(FUNCTION_SLOT_UPDATE_DONE, result.name()));
        return result;
    }

    /**
     * List anonymous HTTP Triggers url after deployment
     */
    protected void listHTTPTriggerUrls() {
        try {
            final List<FunctionResource> triggers = listFunctions();
            final List<FunctionResource> httpFunction = triggers.stream()
                    .filter(function -> function.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                    .collect(Collectors.toList());
            final List<FunctionResource> anonymousTriggers = httpFunction.stream()
                    .filter(bindingResource -> bindingResource.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase((CharSequence) bindingResource.getTrigger().getProperty(AUTH_LEVEL),
                                    AuthorizationLevel.ANONYMOUS.toString()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
                Log.info(NO_ANONYMOUS_HTTP_TRIGGER);
                return;
            }
            Log.info(HTTP_TRIGGER_URLS);
            anonymousTriggers.forEach(trigger -> Log.info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
            if (anonymousTriggers.size() < httpFunction.size()) {
                Log.info(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
            }
        } catch (AzureAuthFailureException | InterruptedException e) {
            Log.warn(FAILED_TO_LIST_TRIGGERS);
        } catch (AzureExecutionException e) {
            Log.warn(e.getMessage());
        }
    }

    protected FunctionRuntimeHandler getFunctionRuntimeHandler() throws AzureAuthFailureException, AzureExecutionException {
        final FunctionRuntimeHandler.Builder<?> builder;
        final OperatingSystemEnum os = getOsEnum();
        validateServicePlanRuntime(os);
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
                .javaVersion(parsedJavaVersion)
                .azure(getAzureClient())
                .build();
    }

    private void validateServicePlanRuntime(OperatingSystemEnum os) throws AzureExecutionException, AzureAuthFailureException {
        final AppServicePlan servicePlan = AppServiceUtils.getAppServicePlan(getAppServicePlanName(), getAzureClient(),
                getResourceGroup(), getAppServicePlanResourceGroup());
        if (servicePlan != null) {
            if ((servicePlan.operatingSystem() == OperatingSystem.LINUX && os == OperatingSystemEnum.Windows) ||
                    (servicePlan.operatingSystem() == OperatingSystem.WINDOWS && os != OperatingSystemEnum.Windows)) {
                throw new AzureExecutionException(String.format("%s service plan can not be used for %s function",
                        StringUtils.capitalize(servicePlan.operatingSystem().name()), StringUtils.lowerCase(os.name())));
            }
        }
    }

    protected OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final String os = runtime == null ? null : runtime.getOs();
        return StringUtils.isEmpty(os) ? RuntimeConfiguration.DEFAULT_OS : Utils.parseOperationSystem(os);
    }

    protected ArtifactHandler getArtifactHandler() throws AzureExecutionException {
        final ArtifactHandlerBase.Builder builder;

        final DeploymentType deploymentType = getDeploymentType();
        getTelemetryProxy().addDefaultProperty(DEPLOYMENT_TYPE_KEY, deploymentType.toString());
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

    protected DeploymentType getDeploymentTypeByRuntime() throws AzureExecutionException {
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

    protected boolean isDedicatedPricingTier() throws AzureExecutionException {
        try {
            final FunctionApp functionApp = getFunctionApp();
            final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlanByAppService(functionApp);
            final PricingTier functionPricingTier = appServicePlan.pricingTier();
            return PricingTier.getAll().stream().anyMatch(pricingTier -> pricingTier.equals(functionPricingTier));
        } catch (AzureAuthFailureException e) {
            throw new AzureExecutionException(FAILED_TO_GET_FUNCTION_APP_PRICING_TIER, e);
        }
    }

    protected void checkArtifactCompileVersion() throws AzureExecutionException {
        if (getOsEnum() == OperatingSystemEnum.Docker) {
            return;
        }
        final ComparableVersion runtimeVersion = new ComparableVersion(parsedJavaVersion.toString());
        final ComparableVersion artifactVersion = new ComparableVersion(Utils.getArtifactCompileVersion(getArtifactToDeploy()));
        if (runtimeVersion.compareTo(artifactVersion) < 0) {
            throw new AzureExecutionException(ARTIFACT_INCOMPATIBLE);
        }
    }

    protected void parseConfiguration() {
        processAppSettingsWithDefaultValue();
        parsedJavaVersion = FunctionUtils.parseJavaVersion(getRuntime().getJavaVersion());
    }

    // todo: Extract validator for all maven toolkits
    @Deprecated
    protected void validateParameters() throws AzureExecutionException {
        // app name
        if (StringUtils.isBlank(appName)) {
            throw new AzureToolkitRuntimeException(EMPTY_APP_NAME);
        }
        if (appName.startsWith("-") || !appName.matches(APP_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(INVALID_APP_NAME);
        }
        // resource group
        if (StringUtils.isBlank(resourceGroup)) {
            throw new AzureToolkitRuntimeException(EMPTY_RESOURCE_GROUP);
        }
        if (resourceGroup.endsWith(".") || !resourceGroup.matches(RESOURCE_GROUP_PATTERN)) {
            throw new AzureToolkitRuntimeException(INVALID_RESOURCE_GROUP_NAME);
        }
        // asp name & resource group
        if (StringUtils.isNotEmpty(appServicePlanName) && !appServicePlanName.matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_SERVICE_PLAN_NAME, APP_SERVICE_PLAN_NAME_PATTERN));
        }
        if (StringUtils.isNotEmpty(appServicePlanResourceGroup) &&
                (appServicePlanResourceGroup.endsWith(".") || !appServicePlanResourceGroup.matches(RESOURCE_GROUP_PATTERN))) {
            throw new AzureToolkitRuntimeException(INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME);
        }
        // slot name
        if (deploymentSlotSetting != null && StringUtils.isEmpty(deploymentSlotSetting.getName())) {
            throw new AzureToolkitRuntimeException(EMPTY_SLOT_NAME);
        }
        if (deploymentSlotSetting != null && !deploymentSlotSetting.getName().matches(SLOT_NAME_PATTERN)) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_SLOT_NAME, SLOT_NAME_PATTERN));
        }
        // region
        if (StringUtils.isNotEmpty(region) && Region.fromName(region) == null) {
            throw new AzureToolkitRuntimeException(INVALID_REGION);
        }
        // image
        if (getOsEnum() == OperatingSystemEnum.Docker && StringUtils.isEmpty(runtime.getImage())) {
            throw new AzureExecutionException(EMPTY_IMAGE_NAME);
        }
    }

    private void processAppSettingsWithDefaultValue() {
        if (appSettings == null) {
            appSettings = new Properties();
        }
        setDefaultAppSetting(appSettings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING);
        setDefaultAppSetting(appSettings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
                FUNCTIONS_EXTENSION_VERSION_VALUE);
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String settingValue) {
        setDefaultAppSetting(result, settingName, settingIsEmptyMessage, settingValue, null);
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String defaultValue, String warningMessage) {
        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            Log.info(settingIsEmptyMessage);
            result.put(settingName, defaultValue);
            return;
        }
        // Show warning message when user set a different value
        if (!StringUtils.equalsIgnoreCase(setting, defaultValue) && StringUtils.isNotEmpty(warningMessage)) {
            Log.warn(warningMessage);
        }
    }

    public FunctionExtensionVersion getFunctionExtensionVersion() throws AzureExecutionException {
        final String extensionVersion = (String) getAppSettings().get(FUNCTIONS_EXTENSION_VERSION_NAME);
        return FunctionUtils.parseFunctionExtensionVersion(extensionVersion);
    }

    protected void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    private WebAppBase.Update updateFunctionAppSettings(WebAppBase.Update update) throws AzureExecutionException, AzureAuthFailureException {
        validateApplicationInsightsConfiguration();
        final Map appSettings = getAppSettings();
        if (isDisableAppInsights()) {
            // Remove App Insights connection when `disableAppInsights` set to true
            // Need to call `withoutAppSetting` as withAppSettings will only not remove parameters
            update.withoutAppSetting(APPINSIGHTS_INSTRUMENTATION_KEY);
        } else {
            bindApplicationInsights(appSettings, false);
        }
        configureAppSettings(update::withAppSettings, appSettings);
        return update;
    }

    private File getArtifactToDeploy() throws AzureExecutionException {
        final File stagingFolder = new File(getDeploymentStagingDirectoryPath());
        return Arrays.stream(Optional.ofNullable(stagingFolder.listFiles()).orElse(new File[0]))
                .filter(jar -> StringUtils.equals(FilenameUtils.getBaseName(jar.getName()), this.getFinalName()))
                .findFirst()
                .orElseThrow(() -> new AzureExecutionException(
                        String.format("Failed to find function artifact '%s.jar' in folder '%s', please re-package the project and try again.",
                                this.getFinalName(),
                                stagingFolder))
                );
    }

    /**
     * Sync triggers and return function list of deployed function app
     * Will retry when get empty result, the max retry times is LIST_TRIGGERS_MAX_RETRY
     * @return List of functions in deployed function app
     * @throws AzureExecutionException Throw if get empty result after LIST_TRIGGERS_MAX_RETRY times retry
     * @throws AzureAuthFailureException Throw if meet Authentication exception while getting Azure client or Function app
     * @throws InterruptedException Throw when thread was interrupted while sleeping between retry
     */
    private List<FunctionResource> listFunctions() throws AzureExecutionException, AzureAuthFailureException, InterruptedException {
        final FunctionApp functionApp = getFunctionApp();
        for (int i = 0; i < LIST_TRIGGERS_MAX_RETRY; i++) {
            Thread.sleep(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS * 1000);
            Log.info(String.format(SYNCING_TRIGGERS_AND_FETCH_FUNCTION_INFORMATION, i + 1, LIST_TRIGGERS_MAX_RETRY));
            try {
                functionApp.syncTriggers();
                final List<FunctionResource> triggers = getAzureClient().appServices().functionApps()
                        .listFunctions(getResourceGroup(), getAppName()).stream()
                        .map(envelope -> FunctionResource.parseFunction(envelope))
                        .filter(function -> function != null)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(triggers)) {
                    return triggers;
                }
            } catch (RuntimeException e) {
                // swallow service exception while list triggers
                continue;
            }
        }
        throw new AzureExecutionException(NO_TRIGGERS_FOUNDED);
    }

    /**
     * Binding Function App with Application Insights
     * Will follow the below sequence appInsightsKey -&gt; appInsightsInstance -&gt; Create New AI Instance (Function creation only)
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

    private boolean isDeployToSlot() {
        final DeploymentSlotSetting slotSetting = getDeploymentSlotSetting();
        return slotSetting != null && StringUtils.isNotEmpty(slotSetting.getName());
    }

    private ApplicationInsightsComponent getOrCreateApplicationInsights(boolean enableCreation) throws AzureAuthFailureException, AzureExecutionException {
        final Account account = Azure.az(AzureAccount.class).account();
        final String subscriptionId = getAzureClient().subscriptionId();
        final ApplicationInsightsManager applicationInsightsManager = new ApplicationInsightsManager(
                account.getTokenCredentialV1(subscriptionId),
                subscriptionId, getUserAgent());
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
            final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
            Log.info(String.format(APPLICATION_INSIGHTS_CREATED, resource.name(), getPortalUrl(environment), resource.id()));
            return resource;
        } catch (Exception e) {
            Log.warn(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, e.getMessage()));
            return null;
        }
    }
}
