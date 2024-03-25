/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.StreamingLogTask;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final String CV2_INVALID_MAX_INSTANCE = "Invalid maximum instances for flex consumption plan, the limit is 1000";
    public static final int MAX_MAX_INSTANCES = 1000;
    public static final Set<Region> FLEX_CONSUMPTION_REGIONS = Collections.unmodifiableSet(Sets.newHashSet(Region.AUSTRALIA_EAST, Region.US_CENTRAL, Region.ASIA_EAST,
        Region.US_EAST, Region.US_EAST2, Region.US_EAST2_EUAP, Region.EUROPE_NORTH, Region.US_SOUTH_CENTRAL, Region.ASIA_SOUTHEAST, Region.EUROPE_SWEDEN_CENTRAL,
        Region.UK_SOUTH, Region.US_WEST2, Region.US_WEST3));

    @Getter
    protected final ConfigParser parser = new ConfigParser(this);

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

    /**
     *  Set the amount of memory allocated to each instance of the function app in MB.
     *  CPU and network bandwidth are allocated proportionally.
     *  Values must be one of 512, 2048, 4096
     *  Default value is 2048
     */
    @JsonProperty
    @Getter
    @Parameter
    protected Integer instanceSize;

    /**
     * The maximum number of instances for the function app.
     * Value must be in range [40, 1000]
     * Default value is 100
     */
    @JsonProperty
    @Getter
    @Parameter
    protected Integer maximumInstances;

    /**
     * The storage account which is used to store deployment artifacts.
     * If not specified, will use account defined with <storageAccountName> for deployment
     */
    @JsonProperty
    @Getter
    @Parameter(property = "deploymentStorageAccount")
    protected String deploymentStorageAccount;

    /**
     * The resource group of the storage account which is used to store deployment artifacts.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "deploymentStorageResourceGroup")
    protected String deploymentStorageResourceGroup;

    /**
     * The container in the storage account which is used to store deployment artifacts.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "deploymentStorageContainer")
    protected String deploymentStorageContainer;

    /**
     * The authentication method to access the storage account for deployment.
     * Available options: SystemAssignedIdentity, UserAssignedIdentity, StorageAccountConnectionString.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "storageAuthenticationMethod")
    protected String storageAuthenticationMethod;

    /**
     * Use this property for UserAssignedIdentity.
     * Set the resource ID of the identity.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "userAssignedIdentityResourceId")
    protected String userAssignedIdentityResourceId;

    /**
     * Use this property for StorageAccountConnectionString.
     * Set the name of the app setting that has the storage account connection string.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "storageAccountConnectionString")
    protected String storageAccountConnectionString;

    /**
     * always ready instances config for flex consumption function app, in the form of name-value pairs.
     * <pre>
     * {@code
     * <alwaysReadyInstances>
     *     <trigger1>value1</trigger1>
     *     <trigger2>value2</trigger2>
     * </alwaysReadyInstances>
     * }
     * </pre>
     *
     * For additional information see https://aka.ms/flexconsumption/alwaysready.
     */
    @JsonProperty
    @Getter
    @Parameter(property = "alwaysReadyInstances")
    protected Map<String, String> alwaysReadyInstances;

    @Override
    @AzureOperation("user/functionapp.deploy_app")
    protected void doExecute() throws Throwable {
        this.mergeCommandLineConfig();
        initAzureAppServiceClient();
        FunctionAppRuntime.tryLoadingAllRuntimes();
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

    protected void doValidate() {
        validateParameters();
        validateFunctionCompatibility();
        validateArtifactCompileVersion();
        validateApplicationInsightsConfiguration();
        if (StringUtils.equalsAnyIgnoreCase(getPricingTier(),
            PricingTier.FLEX_CONSUMPTION.getTier(), PricingTier.FLEX_CONSUMPTION.getSize())) {
            validateFlexConsumptionConfiguration();
        }
        // validate container apps hosting of function app
    }

    private void validateFlexConsumptionConfiguration() {
        // regions
        final Region region = Optional.ofNullable(getRegion()).filter(StringUtils::isNotBlank).map(Region::fromName).orElse(null);
        final String supportedRegions = FLEX_CONSUMPTION_REGIONS.stream().map(Region::getName).collect(Collectors.joining(", "));
        if (Objects.isNull(region)) {
            throw new AzureToolkitRuntimeException("Region is required for flex consumption function app, supported regions are %s", supportedRegions);
        } else if (!FLEX_CONSUMPTION_REGIONS.contains(region)) {
            throw new AzureToolkitRuntimeException("`%s` is not a valid region for flex consumption app, supported values are %s", region.getName(), supportedRegions);
        }
        // runtime
        final RuntimeConfiguration runtimeConfiguration = this.getRuntimeConfiguration();
        final OperatingSystem os = Optional.ofNullable(runtimeConfiguration).map(RuntimeConfiguration::getOs).map(OperatingSystem::fromString).orElse(null);
        if (os != OperatingSystem.LINUX) {
            throw new AzureToolkitRuntimeException("Flex consumption plan only supports Linux runtime");
        }
        // java version
        final String javaVersion = Optional.of(runtimeConfiguration).map(RuntimeConfiguration::getJavaVersion).orElse(null);
        final int javaMajorVersion = Optional.ofNullable(javaVersion).map(FunctionAppLinuxRuntime::fromJavaVersionUserText)
            .map(FunctionAppLinuxRuntime::getJavaMajorVersionNumber).orElse(0);
        if (javaMajorVersion != 11 && javaMajorVersion != 17) {
            throw new AzureToolkitRuntimeException("Flex consumption plan only supports Java 11 and Java 17");
        }
        // scale configuration
        if (Objects.nonNull(instanceSize) && !VALID_CONTAINER_SIZE.contains(instanceSize)) {
            throw new AzureToolkitRuntimeException(String.format(CV2_INVALID_CONTAINER_SIZE, VALID_CONTAINER_SIZE.stream().map(String::valueOf).collect(Collectors.joining(","))));
        }
        if (Objects.nonNull(maximumInstances) && maximumInstances > MAX_MAX_INSTANCES) {
            throw new AzureToolkitRuntimeException(CV2_INVALID_MAX_INSTANCE);
        }
    }

    private void validateArtifactCompileVersion() {
        final RuntimeConfig runtimeConfig = getParser().getRuntimeConfig();
        final String javaVersion = Optional.ofNullable(runtimeConfig).map(RuntimeConfig::javaVersion).orElse(StringUtils.EMPTY);
        validateArtifactCompileVersion(javaVersion, getArtifact(), getFailsOnRuntimeValidationError());
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
    }

    protected FunctionAppBase<?, ?, ?> createOrUpdateResource(final FunctionApp app) throws Throwable {
        final FunctionAppConfig config = parser.parseConfig();
        final boolean newFunctionApp = !app.exists();
        final FunctionAppConfig defaultConfig = !newFunctionApp ?
            fromFunctionApp(app) : buildDefaultConfig(config.subscriptionId(), config.resourceGroup(), config.appName());
        mergeAppServiceConfig(config, defaultConfig);
        if (!newFunctionApp && !config.disableAppInsights() && StringUtils.isEmpty(config.appInsightsKey())) {
            // fill ai key from existing app settings
            Optional.ofNullable(app.getAppSettings())
                .map(map -> map.get(CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY))
                .ifPresent(config::appInsightsKey);
        }
        return new CreateOrUpdateFunctionAppTask(config).doExecute();
    }

    private FunctionAppConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName) {
        final FunctionAppConfig appServiceConfig = AppServiceConfig.buildDefaultFunctionConfig(resourceGroup, appName);
        appServiceConfig.subscriptionId(subscriptionId);
        final List<Region> regions = Azure.az(AzureAppService.class).forSubscription(subscriptionId).listSupportedRegions();
        // replace with first region when the default region is not present
        appServiceConfig.region(selectFirstOptionIfCurrentInvalid("region", regions, appServiceConfig.region()));
        appServiceConfig.setFlexConsumptionConfiguration(FlexConsumptionConfiguration.DEFAULT_CONFIGURATION);
        return appServiceConfig;
    }

    private void deployArtifact(final FunctionAppBase<?, ?, ?> target) {
        final File file = new File(getDeploymentStagingDirectoryPath());
        final FunctionDeployType type = StringUtils.isEmpty(deploymentType) ? null : FunctionDeployType.fromString(deploymentType);
        new DeployFunctionAppTask(target, file, type, true).doExecute();
    }

    private void validateApplicationInsightsConfiguration() {
        if (isDisableAppInsights() && (StringUtils.isNotEmpty(getAppInsightsKey()) || StringUtils.isNotEmpty(getAppInsightsInstance()))) {
            throw new AzureToolkitRuntimeException(APPLICATION_INSIGHTS_CONFIGURATION_CONFLICT);
        }
    }
}
