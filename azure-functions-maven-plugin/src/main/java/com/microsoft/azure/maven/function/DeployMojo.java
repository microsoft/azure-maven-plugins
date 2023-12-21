/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionsServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.StreamingLogTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.fromFunctionApp;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.mergeAppServiceConfig;
import static com.microsoft.azure.toolkit.lib.common.utils.Utils.selectFirstOptionIfCurrentInvalid;

/**
 * Deploy your project to target Azure Functions. If target Function App doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractFunctionMojo {
    private static final String APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT = "Contradictory configurations for application insights," +
            " specify 'appInsightsKey' or 'appInsightsInstance' if you want to enable it, and specify " +
            "'disableAppInsights=true' if you want to disable it.";
    private static final String ARTIFACT_INCOMPATIBLE_WARNING = "Your function app artifact compile version {0} may not compatible with java version {1} in " +
            "configuration.";
    private static final String ARTIFACT_INCOMPATIBLE_ERROR = "Your function app artifact compile version {0} is not compatible with java version {1} in " +
            "configuration, please downgrade the project compile version and try again.";
    private static final String NO_ARTIFACT_FOUNDED = "Failed to find function artifact '%s.jar' in folder '%s', please re-package the project and try again.";
    private static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    private static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9._\\-()]{1,90}";
    private static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private static final List<Integer> VALID_CONTAINER_SIZE = Arrays.asList(512, 2048, 4096);
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
    private static final String EMPTY_IMAGE_NAME = "Please config the <image> of <runtime> in pom.xml.";
    private static final String INVALID_OS = "The value of <os> is not correct, supported values are: windows, linux and docker.";
    private static final String EXPANDABLE_PRICING_TIER_WARNING = "'%s' may not be a valid pricing tier, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-pricing-tiers for valid values";
    private static final String EXPANDABLE_REGION_WARNING = "'%s' may not be a valid region, " +
            "please refer to https://aka.ms/maven_function_configuration#supported-regions for valid values";
    private static final String EXPANDABLE_JAVA_VERSION_WARNING = "'%s' may not be a valid java version, recommended values are `Java 8`, `Java 11` and `Java 17`";
    private static final String CV2_INVALID_CONTAINER_SIZE = "Invalid container size for flex consumption plan, valid values are: %s";
    private static final String CV2_INVALID_RUNTIME = "Windows runtime is not supported within flex consumption service plan";
    private static final String CV2_INVALID_MAX_INSTANCE = "Invalid maximum instances for flex consumption plan, the limit is 1000";
    public static final int MAX_MAX_INSTANCES = 1000;
    public static final String CV2_INVALID_ALWAYS_READY_INSTANCE = "'alwaysReadyInstances' must be less than or equal to 'maximumInstances'";
    public static final String CV2_INVALID_JAVA_VERSION = "Invalid java version for flex consumption plan, only java 17 is supported";

    /**
     * The deployment approach to use, valid values are FTP, ZIP, MSDEPLOY, RUN_FROM_ZIP, RUN_FROM_BLOB <p>
     * For Windows Function Apps, the default deployment method is RUN_FROM_ZIP <p>
     * For Linux Function Apps, RUN_FROM_BLOB will be used for apps with Consumption and Premium App Service Plan,
     * RUN_FROM_ZIP will be used for apps with Dedicated App Service Plan.
     * @since 0.1.0
     */
    @JsonProperty
    @Parameter(property = "deploymentType")
    protected String deploymentType;

    @Override
    @AzureOperation("user/functionapp.deploy_app")
    protected void doExecute() throws Throwable {
        this.mergeCommandLineConfig();
        initAzureAppServiceClient();
        ((FunctionsServiceSubscription) Objects.requireNonNull(Azure.az(AzureWebApp.class).get(subscriptionId, null), "You are not signed-in")).loadRuntimes();
        doValidate();
        final ConfigParser parser = getParser();
        final FunctionAppConfig config = parser.parseConfig();
        final FunctionApp app = Azure.az(AzureFunctions.class).functionApps(config.subscriptionId()).updateOrCreate(config.appName(), config.resourceGroup());
        try {
            final FunctionAppBase<?, ?, ?> target = createOrUpdateResource(app);
            deployArtifact(target);
        } catch (final Exception e) {
            new StreamingLogTask(app).execute();
            throw new AzureToolkitRuntimeException(e);
        }
        updateTelemetryProperties();
    }

    private void mergeCommandLineConfig() {
        try {
            final JavaPropsMapper mapper = new JavaPropsMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            final DeployMojo commandLineConfig = mapper.readSystemPropertiesAs(JavaPropsSchema.emptySchema(), DeployMojo.class);
            Utils.copyProperties(this, commandLineConfig, false);
        } catch (final IOException | IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("failed to merge command line configuration", e);
        }
    }

    protected void doValidate() throws AzureExecutionException {
        validateParameters();
        validateFunctionCompatibility();
        validateArtifactCompileVersion();
        validateApplicationInsightsConfiguration();
    }

    private void validateArtifactCompileVersion() throws AzureExecutionException {
        final RuntimeConfig runtimeConfig = getParser().getRuntimeConfig();
        final FunctionAppRuntime runtime = (FunctionAppRuntime) runtimeConfig.runtime();
        if (Objects.nonNull(runtime)) {
            final String javaVersion = runtime.getJavaVersionNumber();
            validateArtifactCompileVersion(javaVersion, getArtifact(), getFailsOnRuntimeValidationError());
        }
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
        if (StringUtils.isNotEmpty(region) && Region.fromName(region).isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format(EXPANDABLE_REGION_WARNING, region));
        }
        // os
        if (StringUtils.isNotEmpty(runtime.getOs()) && OperatingSystem.fromString(runtime.getOs()) == null) {
            throw new AzureToolkitRuntimeException(INVALID_OS);
        }
        // pricing tier
        if (StringUtils.isNotEmpty(pricingTier) && PricingTier.fromString(pricingTier).isExpandedValue()) {
            AzureMessager.getMessager().warning(String.format(EXPANDABLE_PRICING_TIER_WARNING, pricingTier));
        }
        // docker image
        if (OperatingSystem.fromString(runtime.getOs()) == OperatingSystem.DOCKER && StringUtils.isEmpty(runtime.getImage())) {
            throw new AzureToolkitRuntimeException(EMPTY_IMAGE_NAME);
        }
        // flex consumption
        if (StringUtils.isNotEmpty(pricingTier) && PricingTier.fromString(pricingTier).isFlexConsumption()) {
            if (Objects.nonNull(instanceSize) && !VALID_CONTAINER_SIZE.contains(instanceSize)) {
                throw new AzureToolkitRuntimeException(String.format(CV2_INVALID_CONTAINER_SIZE, VALID_CONTAINER_SIZE.stream().map(String::valueOf).collect(Collectors.joining(","))));
            }
            if (Objects.nonNull(maximumInstances) && maximumInstances > MAX_MAX_INSTANCES) {
                throw new AzureToolkitRuntimeException(CV2_INVALID_MAX_INSTANCE);
            }
            if (ObjectUtils.allNotNull(maximumInstances, alwaysReadyInstances) && alwaysReadyInstances > maximumInstances) {
                throw new AzureToolkitRuntimeException(CV2_INVALID_ALWAYS_READY_INSTANCE);
            }
            if (StringUtils.isEmpty(runtime.getOs()) || OperatingSystem.fromString(runtime.getOs()) == OperatingSystem.WINDOWS) {
                throw new AzureToolkitRuntimeException(CV2_INVALID_RUNTIME);
            }
            if (StringUtils.isNotEmpty(runtime.getJavaVersion()) && Utils.getJavaMajorVersion(runtime.getJavaVersion()) < 17) {
                throw new AzureToolkitRuntimeException(CV2_INVALID_JAVA_VERSION);
            }
        }
    }

    protected FunctionAppBase<?, ?, ?> createOrUpdateResource(final FunctionApp app) throws Throwable {
        final FunctionAppConfig config = parser.parseConfig();
        final boolean newFunctionApp = !app.exists();
        final FunctionAppConfig defaultConfig = !newFunctionApp ?
            fromFunctionApp(app, Objects.requireNonNull(app.getAppServicePlan())) :
            buildDefaultConfig(config.subscriptionId(), config.resourceGroup(), config.appName());
        mergeAppServiceConfig(config, defaultConfig);
        if (!newFunctionApp && !config.disableAppInsights() && StringUtils.isEmpty(config.appInsightsKey())) {
            // fill ai key from existing app settings
            config.appInsightsKey(Objects.requireNonNull(app.getAppSettings()).get(CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY));
        }
        return new CreateOrUpdateFunctionAppTask(config).doExecute();
    }

    private FunctionAppConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName) {
        final FunctionAppConfig appServiceConfig = AppServiceConfig.buildDefaultFunctionConfig(resourceGroup, appName);
        appServiceConfig.subscriptionId(subscriptionId);
        final List<Region> regions = Azure.az(AzureAppService.class).forSubscription(subscriptionId).listSupportedRegions();
        // replace with first region when the default region is not present
        appServiceConfig.region(selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        return appServiceConfig;
    }

    private void deployArtifact(final FunctionAppBase<?, ?, ?> target) {
        final File file = new File(getDeploymentStagingDirectoryPath());
        final FunctionDeployType type = StringUtils.isEmpty(deploymentType) ? null : FunctionDeployType.fromString(deploymentType);
        new DeployFunctionAppTask(target, file, type, true).doExecute();
    }

    private void validateApplicationInsightsConfiguration() throws AzureExecutionException {
        if (isDisableAppInsights() && (StringUtils.isNotEmpty(getAppInsightsKey()) || StringUtils.isNotEmpty(getAppInsightsInstance()))) {
            throw new AzureExecutionException(APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT);
        }
    }
}
