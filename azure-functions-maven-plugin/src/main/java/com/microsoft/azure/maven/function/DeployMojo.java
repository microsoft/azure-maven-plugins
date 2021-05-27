/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsEntity;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure Functions doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {

    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH =
            "Deployment done, you may access your resource through %s";
    private static final String FUNCTION_SLOT_CREATE_START = "The specified function slot does not exist. " +
            "Creating a new slot...";
    private static final String FUNCTION_SLOT_CREATED = "Successfully created the function slot: %s.";
    private static final String FUNCTION_SLOT_UPDATE = "Updating the specified function slot...";
    private static final String FUNCTION_SLOT_UPDATE_DONE = "Successfully updated the function slot: %s.";
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
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer https://aka.ms/azure-functions-key.";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final String ARTIFACT_INCOMPATIBLE = "Your function app artifact compile version is higher than the java version in function host, " +
            "please downgrade the project compile version and try again.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java.";
    private static final String CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING = "App setting `FUNCTIONS_WORKER_RUNTIME` doesn't " +
            "meet the requirement of Azure Java Functions, the value should be `java`.";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
            "isn't configured, setting up the default value.";
    private static final String RUNNING = "Running";
    private static final String CREATE_FUNCTION_APP = "Creating function app %s...";
    private static final String CREATE_FUNCTION_APP_DONE = "Successfully created function app %s.";
    private static final String CREATE_APP_SERVICE_PLAN = "Creating app service plan...";
    private static final String CREATE_APP_SERVICE_DONE = "Successfully created app service plan %s.";
    private static final String CREATE_RESOURCE_GROUP = "Creating resource group %s in region %s...";
    private static final String CREATE_RESOURCE_GROUP_DONE = "Successfully created resource group %s.";
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    private static final String CREATE_NEW_APP_SERVICE_PLAN = "createNewAppServicePlan";
    private static final String CREATE_NEW_RESOURCE_GROUP = "createNewResourceGroup";
    private static final String UPDATE_FUNCTION_APP = "Updating target Function App %s...";
    private static final String UPDATE_FUNCTION_DONE = "Successfully updated Function App %s.";
    private static final String NO_ARTIFACT_FOUNDED = "Failed to find function artifact '%s.jar' in folder '%s', please re-package the project and try again.";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";
    private static final int LIST_TRIGGERS_MAX_RETRY = 3;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String SYNCING_TRIGGERS_AND_FETCH_FUNCTION_INFORMATION = "Syncing triggers and fetching function information (Attempt %d/%d)...";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app, " +
            "please try recompile the project by `mvn clean package` and deploy again.";
    private static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    private static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9._\\-()]{1,90}";
    private static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private static final String EMPTY_APP_NAME = "Please config the <appName> in pom.xml.";
    private static final String INVALID_APP_NAME = "The <appName> only allow alphanumeric characters, hyphens and cannot start or end in a hyphen.";
    private static final String EMPTY_RESOURCE_GROUP = "Please config the <resourceGroup> in pom.xml.";
    private static final String INVALID_RESOURCE_GROUP_NAME = "The <resourceGroup> only allow alphanumeric characters, periods, underscores, " +
            "hyphens and parenthesis and cannot end in a period.";
    private static final String INVALID_SERVICE_PLAN_NAME = "Invalid value for <appServicePlanName>, it need to match the pattern %s";
    private static final String INVALID_SERVICE_PLAN_RESOURCE_GROUP_NAME = "Invalid value for <appServicePlanResourceGroup>, " +
            "it only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final String EMPTY_SLOT_NAME = "Please config the <name> of <deploymentSlot> in pom.xml";
    private static final String INVALID_SLOT_NAME = "Invalid value of <name> inside <deploymentSlot> in pom.xml, it needs to match the pattern '%s'";
    private static final String INVALID_REGION = "The value of <region> is not supported, please correct it in pom.xml.";
    private static final String EMPTY_IMAGE_NAME = "Please config the <image> of <runtime> in pom.xml.";
    private static final String INVALID_OS = "The value of <os> is not correct, supported values are: windows, linux and docker.";
    private static final String INVALID_JAVA_VERSION = "Unsupported value %s for <javaVersion> in pom.xml";
    private static final String INVALID_PRICING_TIER = "Unsupported value %s for <pricingTier> in pom.xml";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";

    private AzureAppService az;

    @Override
    protected void doExecute() throws AzureExecutionException {
        doValidate();
        processAppSettingsWithDefaultValue();

        az = getOrCreateAzureAppServiceClient();
        final IFunctionAppBase target = createOrUpdateResource();

        deployArtifact(target);

        if (target instanceof IFunctionApp) {
            listHTTPTriggerUrls((IFunctionApp) target);
        }
    }

    protected void doValidate() throws AzureExecutionException {
        validateParameters();
        validateArtifactCompileVersion();
        validateApplicationInsightsConfiguration();
    }

    // todo: Extract validator for all maven toolkits
    @Deprecated
    protected void validateParameters() {
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
        // os
        if (StringUtils.isNotEmpty(runtime.getOs()) && OperatingSystem.fromString(runtime.getOs()) == null) {
            throw new AzureToolkitRuntimeException(INVALID_OS);
        }
        // java version
        if (StringUtils.isNotEmpty(runtime.getJavaVersion()) && JavaVersion.fromString(runtime.getJavaVersion()) == JavaVersion.OFF) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_JAVA_VERSION, runtime.getJavaVersion()));
        }
        // pricing tier
        if (StringUtils.isNotEmpty(pricingTier) && PricingTier.fromString(pricingTier) == null) {
            throw new AzureToolkitRuntimeException(String.format(INVALID_PRICING_TIER, pricingTier));
        }
        // docker image
        if (OperatingSystem.fromString(runtime.getOs()) == OperatingSystem.DOCKER && StringUtils.isEmpty(runtime.getImage())) {
            throw new AzureToolkitRuntimeException(EMPTY_IMAGE_NAME);
        }
    }

    protected IFunctionAppBase createOrUpdateResource() throws AzureExecutionException {
        final String deploymentSlotName = Optional.ofNullable(deploymentSlotSetting)
                .map(DeploymentSlotSetting::getName).orElse(null);
        final IFunctionApp functionApp = az.functionApp(getResourceGroup(), getAppName());
        if (StringUtils.isEmpty(deploymentSlotName)) {
            return functionApp.exists() ? updateFunctionApp(functionApp) : createFunctionApp(functionApp);
        } else {
            final IFunctionAppDeploymentSlot slot = functionApp.deploymentSlot(deploymentSlotName);
            return slot.exists() ? updateDeploymentSlot(slot) : createDeploymentSlot(slot);
        }
    }

    protected IFunctionApp createFunctionApp(final IFunctionApp functionApp) throws AzureExecutionException {
        getTelemetryProxy().addDefaultProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
        final ResourceGroup resourceGroup = getOrCreateResourceGroup();
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan();
        AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP, getAppName()));
        final Runtime runtime = getRuntimeOrDefault();
        final Map appSettings = getAppSettings();
        // get/create ai instances only if user didn't specify ai connection string in app settings
        bindApplicationInsights(appSettings, true);
        final IFunctionApp result = (IFunctionApp) functionApp.create().withName(getAppName())
                .withResourceGroup(resourceGroup.getName())
                .withPlan(appServicePlan.id())
                .withRuntime(runtime)
                .withDockerConfiguration(getDockerConfiguration())
                .withAppSettings(appSettings)
                .commit();
        AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP_DONE, result.name()));
        return result;
    }

    private IAppServicePlan getOrCreateAppServicePlan() {
        final String servicePlanName = StringUtils.isEmpty(getAppServicePlanName()) ?
                String.format("asp-%s", getAppName()) : getAppServicePlanName();
        final String servicePlanGroup = getServicePlanResourceGroup();
        final IAppServicePlan appServicePlan = az.appServicePlan(servicePlanGroup, servicePlanName);
        if (!appServicePlan.exists()) {
            AzureMessager.getMessager().info(CREATE_APP_SERVICE_PLAN);
            getTelemetryProxy().addDefaultProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            appServicePlan.create()
                    .withName(servicePlanName)
                    .withResourceGroup(servicePlanGroup)
                    .withRegion(getParsedRegion())
                    .withPricingTier(getParsedPricingTier())
                    .withOperatingSystem(getRuntimeOrDefault().getOperatingSystem())
                    .commit();
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_DONE, appServicePlan.name()));
        }
        return appServicePlan;
    }

    private Region getParsedRegion() {
        return Optional.ofNullable(region).map(Region::fromName).orElse(Region.US_WEST);
    }

    private PricingTier getParsedPricingTier() {
        if (StringUtils.isEmpty(pricingTier)) {
            return PricingTier.CONSUMPTION;
        }
        return Optional.ofNullable(PricingTier.fromString(pricingTier))
                .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("Invalid pricing tier %s", pricingTier)));
    }

    private ResourceGroup getOrCreateResourceGroup() {
        try {
            return Azure.az(AzureGroup.class).getByName(getResourceGroup());
        } catch (ManagementException e) {
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP, getResourceGroup(), getRegion()));
            getTelemetryProxy().addDefaultProperty(CREATE_NEW_RESOURCE_GROUP, String.valueOf(true));
            final ResourceGroup result = Azure.az(AzureGroup.class).create(getResourceGroup(), getRegion());
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP_DONE, result.getName()));
            return result;
        }
    }

    private Runtime getRuntimeOrDefault() {
        final OperatingSystem os = Optional.ofNullable(runtime.getOs()).map(OperatingSystem::fromString).orElse(OperatingSystem.WINDOWS);
        final JavaVersion javaVersion = Optional.ofNullable(runtime.getJavaVersion()).map(JavaVersion::fromString).orElse(JavaVersion.JAVA_8);
        return Runtime.getRuntime(os, WebContainer.JAVA_OFF, javaVersion);
    }

    private Runtime getRuntime() {
        if (StringUtils.isEmpty(runtime.getOs()) && StringUtils.isEmpty(runtime.getJavaVersion())) {
            return null;
        }
        final OperatingSystem os = OperatingSystem.fromString(runtime.getOs());
        final JavaVersion javaVersion = JavaVersion.fromString(runtime.getJavaVersion());
        return Runtime.getRuntime(os, WebContainer.JAVA_OFF, javaVersion);
    }

    private DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        final OperatingSystem os = Optional.ofNullable(runtime.getOs()).map(OperatingSystem::fromString).orElse(null);
        if (os != OperatingSystem.DOCKER) {
            return null;
        }
        final MavenDockerCredentialProvider credentialProvider = MavenDockerCredentialProvider.fromMavenSettings(getSettings(), runtime.getServerId());
        return DockerConfiguration.builder()
                .registryUrl(runtime.getRegistryUrl())
                .image(runtime.getImage())
                .userName(credentialProvider.getUsername())
                .password(credentialProvider.getPassword()).build();
    }

    protected IFunctionApp updateFunctionApp(final IFunctionApp functionApp) throws AzureExecutionException {
        // update app service plan
        AzureMessager.getMessager().info(String.format(UPDATE_FUNCTION_APP, functionApp.name()));
        final IAppServicePlan currentPlan = functionApp.plan();
        IAppServicePlan targetServicePlan = StringUtils.isEmpty(appServicePlanName) ? currentPlan :
                az.appServicePlan(getServicePlanResourceGroup(), appServicePlanName);
        if (!targetServicePlan.exists()) {
            targetServicePlan = getOrCreateAppServicePlan();
        } else if (StringUtils.isNotEmpty(pricingTier)) {
            targetServicePlan.update().withPricingTier(getParsedPricingTier()).commit();
        }
        // update app settings
        final Map<String, String> appSettings = getAppSettings();
        final IAppServiceUpdater<? extends IFunctionApp> update = functionApp.update();
        if (isDisableAppInsights()) {
            update.withoutAppSettings(APPINSIGHTS_INSTRUMENTATION_KEY);
        } else {
            bindApplicationInsights(appSettings, false);
        }
        final IFunctionApp result = update.withPlan(targetServicePlan.id())
                .withRuntime(getRuntime())
                .withDockerConfiguration(getDockerConfiguration())
                .withAppSettings(appSettings)
                .commit();
        AzureMessager.getMessager().info(String.format(UPDATE_FUNCTION_DONE, functionApp.name()));
        return result;
    }

    private String getServicePlanResourceGroup() {
        return StringUtils.isEmpty(getAppServicePlanResourceGroup()) ? getResourceGroup() : getAppServicePlanResourceGroup();
    }

    protected IFunctionAppDeploymentSlot createDeploymentSlot(final IFunctionAppDeploymentSlot deploymentSlot)
            throws AzureExecutionException {
        AzureMessager.getMessager().info(FUNCTION_SLOT_CREATE_START);
        final DeploymentSlotSetting slotSetting = getDeploymentSlotSetting();
        final Map<String, String> appSettings = getAppSettings();
        bindApplicationInsights(appSettings, false);
        final IFunctionAppDeploymentSlot result = deploymentSlot.create().withAppSettings(appSettings)
                .withConfigurationSource(slotSetting.getConfigurationSource())
                .withName(slotSetting.getName()).commit();
        AzureMessager.getMessager().info(String.format(FUNCTION_SLOT_CREATED, result.name()));
        return result;
    }

    protected IFunctionAppDeploymentSlot updateDeploymentSlot(final IFunctionAppDeploymentSlot deploymentSlot) throws AzureExecutionException {
        AzureMessager.getMessager().info(FUNCTION_SLOT_UPDATE);
        final Map<String, String> appSettings = getAppSettings();
        final IFunctionAppDeploymentSlot.Updater update = deploymentSlot.update();
        // todo: remove duplicate codes with update function
        if (isDisableAppInsights()) {
            update.withoutAppSettings(APPINSIGHTS_INSTRUMENTATION_KEY);
        } else {
            bindApplicationInsights(appSettings, false);
        }
        final IFunctionAppDeploymentSlot result = update.withAppSettings(appSettings).commit();
        AzureMessager.getMessager().info(String.format(FUNCTION_SLOT_UPDATE_DONE, result.name()));
        return deploymentSlot;
    }

    private void deployArtifact(IFunctionAppBase target) throws AzureExecutionException {
        AzureMessager.getMessager().info(DEPLOY_START);
        final FunctionDeployType deployType = StringUtils.isEmpty(deploymentType) ? null : FunctionDeployType.fromString(deploymentType);
        // For ftp deploy, we need to upload entire staging directory not the zipped package
        final File file = deployType == FunctionDeployType.FTP ? new File(getDeploymentStagingDirectoryPath()) : packageStagingDirectory();
        final RunnableWithException deployRunnable = deployType == null ? () -> target.deploy(file) : () -> target.deploy(file, deployType);
        executeWithTimeRecorder(deployRunnable, DEPLOY);
        // todo: check function status after deployment
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            target.start();
        }
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, getResourcePortalUrl(target.id())));
    }

    private File packageStagingDirectory() {
        final File zipFile = new File(getDeploymentStagingDirectoryPath() + ".zip");
        final File stagingDirectory = new File(getDeploymentStagingDirectoryPath());

        ZipUtil.pack(stagingDirectory, zipFile);
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }

    /**
     * List anonymous HTTP Triggers url after deployment
     */
    protected void listHTTPTriggerUrls(IFunctionApp target) {
        try {
            final List<FunctionEntity> triggers = listFunctions(target);
            final List<FunctionEntity> httpFunction = triggers.stream()
                    .filter(function -> function.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                    .collect(Collectors.toList());
            final List<FunctionEntity> anonymousTriggers = httpFunction.stream()
                    .filter(bindingResource -> bindingResource.getTrigger() != null &&
                            StringUtils.equalsIgnoreCase(bindingResource.getTrigger().getProperty(AUTH_LEVEL), AuthorizationLevel.ANONYMOUS.toString()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
                AzureMessager.getMessager().info(NO_ANONYMOUS_HTTP_TRIGGER);
                return;
            }
            AzureMessager.getMessager().info(HTTP_TRIGGER_URLS);
            anonymousTriggers.forEach(trigger -> AzureMessager.getMessager().info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
            if (anonymousTriggers.size() < httpFunction.size()) {
                AzureMessager.getMessager().info(UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS);
            }
        } catch (RuntimeException e) {
            // show warning instead of exception for list triggers
            AzureMessager.getMessager().warning(FAILED_TO_LIST_TRIGGERS);
        }
    }

    private List<FunctionEntity> listFunctions(final IFunctionApp functionApp) {
        for (int i = 0; i < LIST_TRIGGERS_MAX_RETRY; i++) {
            try {
                AzureMessager.getMessager().info(String.format(SYNCING_TRIGGERS_AND_FETCH_FUNCTION_INFORMATION, i + 1, LIST_TRIGGERS_MAX_RETRY));
                functionApp.syncTriggers();
                final List<FunctionEntity> triggers = functionApp.listFunctions();
                if (CollectionUtils.isNotEmpty(triggers)) {
                    return triggers;
                }
            } catch (RuntimeException e) {
                // swallow service exception while list triggers
            }
            try {
                Thread.sleep(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS * 1000);
            } catch (InterruptedException e) {
                // swallow interrupted exception
            }
        }
        throw new AzureToolkitRuntimeException(NO_TRIGGERS_FOUNDED);
    }

    protected void validateArtifactCompileVersion() throws AzureExecutionException {
        final Runtime runtime = getRuntimeOrDefault();
        if (runtime.getOperatingSystem() == OperatingSystem.DOCKER) {
            return;
        }
        final ComparableVersion runtimeVersion = new ComparableVersion(runtime.getJavaVersion().getValue());
        final ComparableVersion artifactVersion = new ComparableVersion(Utils.getArtifactCompileVersion(getArtifactToDeploy()));
        if (runtimeVersion.compareTo(artifactVersion) < 0) {
            throw new AzureExecutionException(ARTIFACT_INCOMPATIBLE);
        }
    }

    public void processAppSettingsWithDefaultValue() {
        if (appSettings == null) {
            appSettings = new Properties();
        }
        setDefaultAppSetting(appSettings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CUSTOMIZED_FUNCTIONS_WORKER_RUNTIME_WARNING);
        setDefaultAppSetting(appSettings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
                FUNCTIONS_EXTENSION_VERSION_VALUE, null);
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String defaultValue, String warningMessage) {
        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            AzureMessager.getMessager().info(settingIsEmptyMessage);
            result.put(settingName, defaultValue);
            return;
        }
        // Show warning message when user set a different value
        if (!StringUtils.equalsIgnoreCase(setting, defaultValue) && StringUtils.isNotEmpty(warningMessage)) {
            AzureMessager.getMessager().warning(warningMessage);
        }
    }

    private File getArtifactToDeploy() throws AzureExecutionException {
        final File stagingFolder = new File(getDeploymentStagingDirectoryPath());
        return Arrays.stream(Optional.ofNullable(stagingFolder.listFiles()).orElse(new File[0]))
                .filter(jar -> StringUtils.equals(FilenameUtils.getBaseName(jar.getName()), this.getFinalName()))
                .findFirst()
                .orElseThrow(() -> new AzureExecutionException(String.format(NO_ARTIFACT_FOUNDED, this.getFinalName(), stagingFolder)));
    }

    /**
     * Binding Function App with Application Insights
     * Will follow the below sequence appInsightsKey -&gt; appInsightsInstance -&gt; Create New AI Instance (Function creation only)
     *
     * @param appSettings App settings map
     * @param isCreation  Define the stage of function app, as we only create ai instance by default when create new function apps
     * @throws AzureExecutionException When there are conflicts in configuration or meet errors while finding/creating application insights instance
     */
    private void bindApplicationInsights(Map appSettings, boolean isCreation) throws AzureExecutionException {
        // Skip app insights creation when user specify ai connection string in app settings
        if (appSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY)) {
            return;
        }
        final String instrumentationKey;
        if (StringUtils.isNotEmpty(getAppInsightsKey())) {
            instrumentationKey = getAppInsightsKey();
            if (!Utils.isGUID(instrumentationKey)) {
                throw new AzureExecutionException(INSTRUMENTATION_KEY_IS_NOT_VALID);
            }
        } else {
            final ApplicationInsightsEntity applicationInsightsComponent = getOrCreateApplicationInsights(isCreation);
            instrumentationKey = applicationInsightsComponent == null ? null : applicationInsightsComponent.getInstrumentationKey();
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

    private ApplicationInsightsEntity getOrCreateApplicationInsights(boolean enableCreation) {
        return StringUtils.isNotEmpty(getAppInsightsInstance()) ? getApplicationInsights(getAppInsightsInstance()) :
                enableCreation ? createApplicationInsights(getAppName()) : null;
    }

    private ApplicationInsightsEntity getApplicationInsights(String appInsightsInstance) {
        ApplicationInsightsEntity resource;
        try {
            resource = Azure.az(ApplicationInsights.class).get(getResourceGroup(), appInsightsInstance);
        } catch (ManagementException e) {
            resource = null;
        }
        if (resource == null) {
            AzureMessager.getMessager().warning(String.format(FAILED_TO_GET_APPLICATION_INSIGHTS, appInsightsInstance, getResourceGroup()));
            return createApplicationInsights(appInsightsInstance);
        }
        return resource;
    }

    private ApplicationInsightsEntity createApplicationInsights(String name) {
        if (isDisableAppInsights()) {
            AzureMessager.getMessager().info(SKIP_CREATING_APPLICATION_INSIGHTS);
            return null;
        }
        try {
            AzureMessager.getMessager().info(APPLICATION_INSIGHTS_CREATE_START);
            final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
            final ApplicationInsightsEntity resource = Azure.az(ApplicationInsights.class).create(getResourceGroup(), Region.fromName(getRegion()), name);
            AzureMessager.getMessager().info(String.format(APPLICATION_INSIGHTS_CREATED, resource.getName(), getPortalUrl(environment), resource.getId()));
            return resource;
        } catch (Exception e) {
            AzureMessager.getMessager().warning(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, e.getMessage()));
            return null;
        }
    }
}
