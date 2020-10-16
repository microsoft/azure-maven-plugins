/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.Utils;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.queryer.MavenPluginQueryer;
import com.microsoft.azure.maven.queryer.QueryFactory;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.WebAppPomHandler;
import com.microsoft.azure.maven.webapp.models.SubscriptionOption;
import com.microsoft.azure.maven.webapp.models.WebAppOption;
import com.microsoft.azure.maven.webapp.parser.V2NoValidationConfigurationParser;
import com.microsoft.azure.maven.webapp.utils.CustomTextIoStringListReader;
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;
import com.microsoft.azure.maven.webapp.utils.WebContainerUtils;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.dom4j.DocumentException;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import static com.microsoft.azure.maven.webapp.utils.Utils.findStringInCollectionIgnoreCase;
import static com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator.APP_NAME_PATTERN;
import static com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator.RESOURCE_GROUP_PATTERN;
import static com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator.SUBSCRIPTION_ID_PATTERN;

/**
 * Init or edit the configuration of azure webapp maven plugin.
 */
@Mojo(name = "config")
public class ConfigMojo extends AbstractWebAppMojo {
    private static final String WEB_CONTAINER = "webContainer";
    private static final String JAVA_VERSION = "javaVersion";
    private static final String COMMON_PROMPT = "Define value for %s [%s]:";
    private static final String PRICING_TIER_PROMPT = "Define value for pricingTier [%s]:";
    private static final String NOT_EMPTY_REGEX = "[\\s\\S]+";
    private static final String BOOLEAN_REGEX = "[YyNn]";

    private static final String CONFIG_ONLY_SUPPORT_V2 = "Config only support V2 schema";
    private static final String CHANGE_OS_WARNING = "The plugin may not work if you change the os of an existing " +
        "webapp.";
    private static final String CONFIGURATION_NO_RUNTIME = "No runtime configuration, skip it.";
    private static final String SAVING_TO_POM = "Saving configuration to pom.";

    private static final String PRICE_TIER_NOT_AVAIL = "The price tier \"P1\", \"P2\", \"P3\" are only available for Windows runtime, use \"%s\" instead.";
    private static final String NO_JAVA_WEB_APPS = "There are no Java Web Apps in current subscription, please follow the following steps to create a new one.";
    private static final String LONG_LOADING_HINT = "It may take a few minutes to load all Java Web Apps, please be patient.";
    private static final String[] configTypes = { "Application", "Runtime", "DeploymentSlot" };
    private static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    private static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";
    private static final String SETTING_REGISTRY_USERNAME = "DOCKER_REGISTRY_SERVER_USERNAME";
    private static final String SERVER_ID_TEMPLATE = "Please add a server in Maven settings.xml related to username: %s and put the serverId here";

    private MavenPluginQueryer queryer;
    private WebAppPomHandler pomHandler;

    @Override
    protected void doExecute() throws AzureExecutionException {
        if (!(Utils.isPomPackagingProject(this.project.getPackaging()) ||
                Utils.isJarPackagingProject(this.project.getPackaging()) ||
                Utils.isEarPackagingProject(this.project.getPackaging()) ||
                Utils.isWarPackagingProject(this.project.getPackaging()))) {
            throw new UnsupportedOperationException(
                    String.format("The project (%s) with packaging %s is not supported for azure app service.",
                            this.project.getName(), this.project.getPackaging()));
        }

        queryer = QueryFactory.getQueryer(settings);
        try {
            pomHandler = new WebAppPomHandler(project.getFile().getAbsolutePath());
            final WebAppConfiguration configuration = pomHandler.getConfiguration() == null ? null :
                    getWebAppConfigurationWithoutValidation();
            if (!isV2Configuration(configuration)) {
                Log.warn(CONFIG_ONLY_SUPPORT_V2);
            } else {
                config(configuration);
            }
        } catch (DocumentException | MojoFailureException | IOException | IllegalAccessException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        } finally {
            queryer.close();
        }
    }

    private boolean isV2Configuration(WebAppConfiguration configuration) {
        return configuration == null || schemaVersion.equalsIgnoreCase(SchemaVersion.V2.toString());
    }

    protected void config(WebAppConfiguration configuration) throws MojoFailureException, AzureExecutionException,
        IOException, IllegalAccessException {
        WebAppConfiguration result = null;
        do {
            if (configuration == null) {
                try {
                    result = chooseExistingWebappForConfiguration();
                    if (result == null) {
                        result = initConfig();
                    }
                } catch (AzureAuthFailureException e) {
                    throw new AzureExecutionException(
                            String.format("Cannot get Web App list due to error: %s.", e.getMessage()), e);
                }

            } else {
                result = updateConfiguration(configuration.getBuilderFromConfiguration().build());
            }
        } while (!confirmConfiguration(result));
        Log.info(SAVING_TO_POM);
        // legacy code: to distinguish the non-value changes of javaVersion and webContainer: eg: jre8 to Java 8, jre8 -> Java SE
        if (Objects.nonNull(configuration) && Objects.nonNull(result.getOs()) && Objects.nonNull(this.getRuntime())) {
            switch (result.getOs()) {
                case Linux:
                    if (!StringUtils.equals(
                            RuntimeStackUtils.getJavaVersionFromRuntimeStack(result.getRuntimeStack()),
                            this.getRuntime().getJavaVersionRaw()) ||
                        !StringUtils.equals(
                                RuntimeStackUtils.getWebContainerFromRuntimeStack(result.getRuntimeStack()),
                                this.getRuntime().getWebContainerRaw())) {
                        FieldUtils.writeField(configuration, "runtimeStack", null, true);
                    }
                    break;
                case Windows:
                    if (!StringUtils.equals(
                            JavaVersionUtils.formatJavaVersion(result.getJavaVersion()),
                            this.getRuntime().getJavaVersionRaw())) {
                        FieldUtils.writeField(configuration, JAVA_VERSION, null, true);
                    }
                    if (!StringUtils.equals(
                            WebContainerUtils.formatWebContainer(result.getWebContainer()),
                            this.getRuntime().getWebContainerRaw())) {
                        FieldUtils.writeField(configuration, WEB_CONTAINER, null, true);
                    }
                    break;
                default:
                    break;
            }
        }

        pomHandler.updatePluginConfiguration(result, configuration);
    }

    protected boolean confirmConfiguration(WebAppConfiguration configuration) throws AzureExecutionException,
        MojoFailureException {
        System.out.println("Please confirm webapp properties");
        if (StringUtils.isNotBlank(configuration.getSubscriptionId())) {
            System.out.println("Subscription Id : " + configuration.getSubscriptionId());
        }
        System.out.println("AppName : " + configuration.getAppName());
        System.out.println("ResourceGroup : " + configuration.getResourceGroup());
        System.out.println("Region : " + configuration.getRegion());
        System.out.println("PricingTier : " + configuration.getPricingTier());

        if (configuration.getOs() == null) {
            System.out.println(CONFIGURATION_NO_RUNTIME);
        } else {
            System.out.println("OS : " + configuration.getOs().toString());
            switch (configuration.getOs()) {
                case Windows:
                    System.out.println("Java : " + JavaVersionUtils.formatJavaVersion(configuration.getJavaVersion()));
                    System.out.println("Web server stack: " + WebContainerUtils.formatWebContainer(configuration.getWebContainer()));
                    break;
                case Linux:
                    System.out.println(
                            "Java : " + RuntimeStackUtils.getJavaVersionFromRuntimeStack(configuration.getRuntimeStack()));
                    System.out.println("Web server stack: " +
                            RuntimeStackUtils.getWebContainerFromRuntimeStack(configuration.getRuntimeStack()));
                    break;
                case Docker:
                    System.out.println("Image : " + configuration.getImage());
                    System.out.println("ServerId : " + configuration.getServerId());
                    System.out.println("RegistryUrl : " + configuration.getRegistryUrl());
                    break;
                default:
                    throw new AzureExecutionException("The value of <os> is unknown.");
            }
        }
        System.out.println("Deploy to slot : " + (configuration.getDeploymentSlotSetting() != null));
        if (configuration.getDeploymentSlotSetting() != null) {
            final DeploymentSlotSetting slotSetting = configuration.getDeploymentSlotSetting();
            System.out.println("Slot name : " + slotSetting.getName());
            System.out.println("ConfigurationSource : " + slotSetting.getConfigurationSource());
        }
        final String result = queryer.assureInputFromUser("confirm", "Y", BOOLEAN_REGEX, "Confirm (Y/N)", null);
        return result.equalsIgnoreCase("Y");
    }

    protected WebAppConfiguration initConfig() throws MojoFailureException, AzureExecutionException {
        final WebAppConfiguration result = getDefaultConfiguration();
        return getRuntimeConfiguration(result, true);
    }

    private WebAppConfiguration getDefaultConfiguration() throws AzureExecutionException {
        final WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        final String defaultName = getProject().getArtifactId() + "-" + System.currentTimeMillis();
        final String resourceGroup = defaultName + "-rg";
        final String defaultSchemaVersion = "v2";
        final Region defaultRegion = WebAppConfiguration.DEFAULT_REGION;
        final PricingTier pricingTier = WebAppConfiguration.DEFAULT_PRICINGTIER;
        return builder.appName(defaultName)
            .subscriptionId(subscriptionId)
            .resourceGroup(resourceGroup)
            .region(defaultRegion)
            .pricingTier(pricingTier)
            .resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources())
            .schemaVersion(defaultSchemaVersion)
            .subscriptionId(this.subscriptionId)
            .build();
    }

    protected WebAppConfiguration updateConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException, AzureExecutionException {
        final String selection = queryer.assureInputFromUser("selection", configTypes[0], Arrays.asList(configTypes),
            String.format("Please choose which part to config [%s]:", configTypes[0]));
        switch (selection) {
            case "Application":
                return getWebAppConfiguration(configuration);
            case "Runtime":
                Log.warn(CHANGE_OS_WARNING);
                return getRuntimeConfiguration(configuration, false);
            case "DeploymentSlot":
                return getSlotConfiguration(configuration);
            default:
                throw new AzureExecutionException("Unknown webapp setting");
        }
    }

    private WebAppConfiguration getWebAppConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException {
        final WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();

        final String defaultSubscriptionId = StringUtils.isNotBlank(configuration.subscriptionId) ? configuration.subscriptionId : null;
        final String subscriptionId = StringUtils.isNotBlank(defaultSubscriptionId) ? queryer.assureInputFromUser("subscriptionId", defaultSubscriptionId,
                SUBSCRIPTION_ID_PATTERN, null, null) : null;

        final String defaultAppName =
                getDefaultValue(configuration.appName, getProject().getArtifactId(), APP_NAME_PATTERN);
        final String appName = queryer.assureInputFromUser("appName", defaultAppName,
                APP_NAME_PATTERN, null, null);

        final String defaultResourceGroup = getDefaultValue(configuration.resourceGroup,
                String.format("%s-rg", appName), RESOURCE_GROUP_PATTERN);
        final String resourceGroup = queryer.assureInputFromUser("resourceGroup",
            defaultResourceGroup,
                RESOURCE_GROUP_PATTERN, null, null);

        final String defaultRegion = configuration.getRegionOrDefault();
        final String region = queryer.assureInputFromUser("region", defaultRegion, NOT_EMPTY_REGEX,
            null, null);

        final String currentPricingTier = configuration.getPricingTierOrDefault();
        final List<String> availablePriceList = getAvailablePricingTierList(configuration.getOs());
        String defaultPricingTier = currentPricingTier;
        if (availablePriceList.stream().noneMatch(price -> StringUtils.equalsIgnoreCase(price, currentPricingTier))) {
            defaultPricingTier = AppServiceUtils.convertPricingTierToString(WebAppConfiguration.DEFAULT_PRICINGTIER);
            Log.warn(String.format(PRICE_TIER_NOT_AVAIL, defaultPricingTier));
        }

        final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultPricingTier,
            availablePriceList, String.format(PRICING_TIER_PROMPT, defaultPricingTier));
        return builder
            .subscriptionId(subscriptionId)
            .appName(appName)
            .resourceGroup(resourceGroup)
            .region(Region.fromName(region))
            .pricingTier(AppServiceUtils.getPricingTierFromString(pricingTier))
            .build();
    }

    private WebAppConfiguration getSlotConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException {
        final WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();

        final DeploymentSlotSetting deploymentSlotSetting = configuration.getDeploymentSlotSetting();
        final String defaultIsSlotDeploy = deploymentSlotSetting == null ? "N" : "Y";
        final String isSlotDeploy = queryer.assureInputFromUser("isSlotDeploy", defaultIsSlotDeploy, BOOLEAN_REGEX,
            "Deploy to slot?(Y/N)", null);
        if (isSlotDeploy.toLowerCase().equals("n")) {
            return builder.deploymentSlotSetting(null).build();
        }

        final String defaultSlotName = deploymentSlotSetting == null ? String.format("%s-slot",
            configuration.getAppName()) : deploymentSlotSetting.getName();
        final String slotName = queryer.assureInputFromUser("slotName", defaultSlotName, NOT_EMPTY_REGEX,
            null, null);

        final String defaultConfigurationSource = deploymentSlotSetting == null ? null :
            deploymentSlotSetting.getConfigurationSource();
        final String configurationSource = queryer.assureInputFromUser("configurationSource",
            defaultConfigurationSource, null, null, null);

        final DeploymentSlotSetting result = new DeploymentSlotSetting();
        result.setName(slotName);
        result.setConfigurationSource(configurationSource);
        return builder.deploymentSlotSetting(result).build();
    }

    private WebAppConfiguration getRuntimeConfiguration(WebAppConfiguration configuration, boolean initial)
        throws MojoFailureException, AzureExecutionException {
        WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();
        final OperatingSystemEnum defaultOs = configuration.getOs() == null ? OperatingSystemEnum.Linux :
            configuration.getOs();
        final String os = queryer.assureInputFromUser("OS", defaultOs, String.format("Define value for OS [%s]:", defaultOs.toString()));
        builder.os(Utils.parseOperationSystem(os));
        if (initial || pricingTierNotSupport(Utils.parseOperationSystem(os), configuration.getPricingTier())) {
            String defaultPricingTier = configuration.getPricingTierOrDefault();
            final List<String> availablePriceList = getAvailablePricingTierList(Utils.parseOperationSystem(os));
            if (!availablePriceList.contains(defaultPricingTier)) {
                Log.warn(String.format("'%s' is not supported in '%s'", defaultPricingTier, os));
                defaultPricingTier = AppServiceUtils.convertPricingTierToString(WebAppConfiguration.DEFAULT_PRICINGTIER);
            }
            final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultPricingTier, availablePriceList,
                    String.format(PRICING_TIER_PROMPT, defaultPricingTier));
            builder.pricingTier(AppServiceUtils.getPricingTierFromString(pricingTier));
        }
        switch (os.toLowerCase()) {
            case "linux":
                builder = getRuntimeConfigurationOfLinux(builder, configuration);
                break;
            case "windows":
                builder = getRuntimeConfigurationOfWindows(builder, configuration);
                break;
            case "docker":
                builder = getRuntimeConfigurationOfDocker(builder, configuration);
                break;
            default:
                throw new AzureExecutionException("The value of <os> is unknown.");
        }
        return builder.build();
    }

    private static boolean pricingTierNotSupport(OperatingSystemEnum parseOperationSystem, PricingTier pricingTier) {
        final List<String> availablePriceList = getAvailablePricingTierList(parseOperationSystem);
        return Objects.isNull(findStringInCollectionIgnoreCase(availablePriceList, AppServiceUtils.convertPricingTierToString(pricingTier)));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfLinux(WebAppConfiguration.Builder builder,
            WebAppConfiguration configuration) throws MojoFailureException {
        String defaultJavaVersion = configuration.getLinuxJavaVersionOrDefault();
        // sometimes the combination of <javaVersion> and <webContainer> may not be valid, but
        // <javaVersion> might be right
        if (Objects.isNull(configuration.getRuntimeStack()) && Objects.nonNull(this.getRuntime()) &&
                Objects.nonNull(this.getRuntime().getJavaVersion())) {
            defaultJavaVersion = JavaVersionUtils.formatJavaVersion(this.getRuntime().getJavaVersion());
        }
        final String javaVersion = queryer.assureInputFromUser(JAVA_VERSION, defaultJavaVersion,
                JavaVersionUtils.getValidJavaVersions(), String.format(COMMON_PROMPT, JAVA_VERSION, defaultJavaVersion));
        // For project which package is jar, use java se runtime
        if (isJarProject()) {
            return builder.runtimeStack(RuntimeStackUtils.getJavaSERuntimeStack(javaVersion));
        }
        final List<String> validRuntimeStacks = RuntimeStackUtils.getValidLinuxRuntimeStacksForJavaVersion(javaVersion);
        String defaultLinuxRuntimeStack = configuration.getLinuxRuntimeStackOrDefault();
        // sometimes the combination of <javaVersion> and <webContainer> may not be valid, but
        // <webContainer> might be right
        if (Objects.isNull(configuration.getRuntimeStack()) && Objects.nonNull(this.getRuntime()) &&
                 StringUtils.isNotBlank(this.getRuntime().getWebContainerRaw())) {
            final String validRuntStackFromExistingConfiguration = findStringInCollectionIgnoreCase(validRuntimeStacks, this.getRuntime().getWebContainerRaw());
            if (validRuntStackFromExistingConfiguration == null) {
                Log.warn(String.format("Invalid webContainer '%s' for java version: %s",
                        this.getRuntime().getWebContainerRaw(), javaVersion));
            } else {
                defaultLinuxRuntimeStack = validRuntStackFromExistingConfiguration;
            }
        } else {
            if (Objects.isNull(findStringInCollectionIgnoreCase(validRuntimeStacks, defaultLinuxRuntimeStack))) {
                Log.warn(String.format("'%s' is not supported in java version: %s", defaultLinuxRuntimeStack, javaVersion));
                defaultLinuxRuntimeStack = WebAppConfiguration.DEFAULT_LINUX_WEB_CONTAINER;
            }
        }
        final String runtimeStack = queryer.assureInputFromUser("runtimeStack", defaultLinuxRuntimeStack,
                validRuntimeStacks, null);
        return builder.runtimeStack(RuntimeStackUtils.getRuntimeStack(javaVersion, runtimeStack));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfWindows(WebAppConfiguration.Builder builder,
            WebAppConfiguration configuration) throws MojoFailureException {
        String defaultJavaVersion = JavaVersionUtils.formatJavaVersion(configuration.getJavaVersionOrDefault());
        final List<String> validJavaVersions = JavaVersionUtils.getValidJavaVersions();
        if (!validJavaVersions.contains(defaultJavaVersion)) {
            Log.warn(String.format("'%s' is not supported.", defaultJavaVersion));
            defaultJavaVersion = WebAppConfiguration.DEFAULT_LINUX_JAVA_VERSION;
        }

        final String javaVersionInput = queryer.assureInputFromUser(JAVA_VERSION, defaultJavaVersion,
                validJavaVersions, String.format(COMMON_PROMPT, JAVA_VERSION, defaultJavaVersion));
        final JavaVersion javaVersion = JavaVersionUtils.toAzureSdkJavaVersion(javaVersionInput);
        // For project which package is jar, use java se runtime
        if (isJarProject()) {
            final WebContainer webContainer = WebContainerUtils.getJavaSEWebContainer();
            return builder.javaVersion(javaVersion).webContainer(webContainer);
        }
        final String defaultWebContainer = configuration.getWebContainerOrDefault();
        final String webContainerInput = queryer.assureInputFromUser(WEB_CONTAINER, defaultWebContainer,
                WebContainerUtils.getAvailableWebContainer(), String.format(COMMON_PROMPT, WEB_CONTAINER, defaultWebContainer));
        return builder.javaVersion(javaVersion).webContainer(WebContainer.fromString(webContainerInput));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfDocker(WebAppConfiguration.Builder builder,
            WebAppConfiguration configuration) throws MojoFailureException {
        final String image = queryer.assureInputFromUser("image", configuration.image, NOT_EMPTY_REGEX, null, null);
        final String serverId = queryer.assureInputFromUser("serverId", configuration.serverId, null, null, null);
        final String registryUrl = queryer.assureInputFromUser("registryUrl", configuration.registryUrl, null, null,
                null);
        return builder.image(image).serverId(serverId).registryUrl(registryUrl);
    }

    private static List<String> getAvailablePricingTierList(OperatingSystemEnum operatingSystem) {
        final Set<String> pricingTierSet = new HashSet<>();
        // Linux and docker app service uses linux as the os of app service plan.
        final List<PricingTier> availablePricingTier = AppServiceUtils.getAvailablePricingTiers(
                operatingSystem == OperatingSystemEnum.Windows ? OperatingSystem.WINDOWS : OperatingSystem.LINUX);
        for (final PricingTier pricingTier : availablePricingTier) {
            pricingTierSet.add(pricingTier.toSkuDescription().size());
        }
        final List<String> result = new ArrayList<>(pricingTierSet);
        Collections.sort(result);
        return result;
    }

    private String getDefaultValue(String defaultValue, String fallBack, String pattern) {
        return StringUtils.isNotEmpty(defaultValue) && defaultValue.matches(pattern) ? defaultValue : fallBack;
    }

    private boolean isJarProject() {
        return Utils.isJarPackagingProject(project.getPackaging());
    }

    private WebAppConfiguration getWebAppConfigurationWithoutValidation() throws AzureExecutionException {
        return new V2NoValidationConfigurationParser(this, new V2ConfigurationValidator(this)).getWebAppConfiguration();
    }

    private WebAppConfiguration chooseExistingWebappForConfiguration()
            throws AzureExecutionException, AzureAuthFailureException {
        final Azure az = getAzureClientByAuthType();
        final TextIO textIO = TextIoFactory.getTextIO();
        final Subscription[] subscriptions = az.subscriptions().list().toArray(new Subscription[0]);
        final Subscription targetSubscription = selectSubscription(az, textIO, subscriptions);
        this.subscriptionId = targetSubscription.subscriptionId();
        // here is a walk around to solve the bad app service listing issue
        final WebAppsInner webappClient = az.webApps().manager().inner().withSubscriptionId(subscriptionId).webApps();
        final List<WebAppOption> siteInners = webappClient.list().stream()
              .filter(site -> site.kind() != null && !Arrays.asList(site.kind().split(",")).contains("functionapp"))
              .map(t -> new WebAppOption(t, webappClient)).sorted().collect(Collectors.toList());

        // check empty: first time
        if (siteInners.isEmpty()) {
            Log.warn(NO_JAVA_WEB_APPS);
            return null;
        }
        Log.info(LONG_LOADING_HINT);

        // load configuration to detecting java or docker
        Observable.from(siteInners).flatMap(WebAppOption::loadConfigurationSync, siteInners.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();

        final boolean isContainer = !Utils.isJarPackagingProject(this.project.getPackaging());
        final boolean isDockerOnly = Utils.isPomPackagingProject(this.project.getPackaging());
        final List<WebAppOption> javaOrDockerWebapps = siteInners.stream().filter(app -> app.isJavaWebApp() || app.isDockerWebapp())
                .filter(app -> checkWebAppVisible(isContainer, isDockerOnly, app.isJavaSE(), app.isDockerWebapp())).sorted()
                .collect(Collectors.toList());
        final WebAppOption selectedApp = selectAzureWebApp(textIO, javaOrDockerWebapps,
                getWebAppTypeByPackaging(this.project.getPackaging()), targetSubscription);
        if (selectedApp == null || selectedApp.isCreateNew()) {
            return null;
        }

        final WebApp webapp = az.webApps().manager().webApps().getById(selectedApp.getId());
        final String serverPlanId = selectedApp.getServicePlanId();
        AppServicePlan servicePlan = null;
        if (StringUtils.isNotBlank(serverPlanId)) {
            servicePlan = az.webApps().manager().appServicePlans().getById(serverPlanId);
        }

        final WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        if (!AppServiceUtils.isDockerAppService(webapp)) {
            builder.resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources());
        }
        return getConfigurationFromExisting(webapp, servicePlan, builder);
    }

    private static Subscription selectSubscription(Azure az, TextIO textIO, Subscription[]subscriptions) throws AzureExecutionException {
        if (subscriptions.length == 0) {
            throw new AzureExecutionException("Cannot find any subscriptions in current account.");
        } else if (subscriptions.length == 1) {
            Log.info(String.format("There is only one subscription '%s' in your account, will use it automatically.",
                    TextUtils.blue(SubscriptionOption.getSubscriptionName(subscriptions[0]))));
            return subscriptions[0];
        } else {
            final String defaultId = Optional.ofNullable(az.getCurrentSubscription()).map(Subscription::subscriptionId).orElse(null);

            final List<SubscriptionOption> wrapSubs = Arrays.stream(subscriptions).map(t -> new SubscriptionOption(t))
                    .sorted()
                    .collect(Collectors.toList());
            final SubscriptionOption defaultValue = wrapSubs.stream()
                    .filter(t -> StringUtils.equalsIgnoreCase(t.getSubscriptionId(), defaultId)).findFirst().orElse(null);

            final SubscriptionOption subscriptionOptionSelected = new CustomTextIoStringListReader<SubscriptionOption>(() -> textIO.getTextTerminal(), null)
                    .withCustomPrompt(String.format("Please choose a subscription%s: ",
                            highlightDefaultValue(defaultValue == null ? null : defaultValue.getSubscriptionName())))
                    .withNumberedPossibleValues(wrapSubs).withDefaultValue(defaultValue).read("Available subscriptions:");
            if (subscriptionOptionSelected == null) {
                throw new AzureExecutionException("You must select a subscription.");
            }
            return subscriptionOptionSelected.getSubscription();
        }
    }

    private static WebAppOption selectAzureWebApp(TextIO textIO, List<WebAppOption> javaOrDockerWebapps, String webAppType, Subscription targetSubscription) {
        final List<WebAppOption> options = new ArrayList<>();
        options.add(WebAppOption.CREATE_NEW);
        // check empty: second time
        if (javaOrDockerWebapps.isEmpty()) {
            Log.warn(NO_JAVA_WEB_APPS);
            return null;
        }
        options.addAll(javaOrDockerWebapps);
        return new CustomTextIoStringListReader<WebAppOption>(() -> textIO.getTextTerminal(), null)
                .withCustomPrompt(String.format("Please choose a %s Web App%s: ", webAppType, highlightDefaultValue(WebAppOption.CREATE_NEW.toString())))
                .withNumberedPossibleValues(options).withDefaultValue(WebAppOption.CREATE_NEW)
                .read(String.format("%s Web Apps in subscription %s:", webAppType, TextUtils.blue(targetSubscription.displayName())));
    }

    private static WebAppConfiguration getConfigurationFromExisting(WebApp webapp, AppServicePlan servicePlan, WebAppConfiguration.Builder builder) {
        // common configuration
        builder.appName(webapp.name())
                .resourceGroup(webapp.resourceGroupName())
                .subscriptionId(Utils.getSubscriptionId(webapp.id()))
                .region(webapp.region());

        if (AppServiceUtils.isDockerAppService(webapp)) {
            builder.os(OperatingSystemEnum.Docker);
            final Map<String, AppSetting> settings = webapp.getAppSettings();

            final AppSetting imageSetting = settings.get(SETTING_DOCKER_IMAGE);
            if (imageSetting != null && StringUtils.isNotBlank(imageSetting.value())) {
                builder.image(imageSetting.value());
            } else {
                builder.image(getDockerImageName(webapp.linuxFxVersion()));
            }
            final AppSetting registryServerSetting = settings.get(SETTING_REGISTRY_SERVER);
            if (registryServerSetting != null && StringUtils.isNotBlank(registryServerSetting.value())) {
                builder.registryUrl(registryServerSetting.value());
            }

            final AppSetting dockerUsernameSetting = settings.get(SETTING_REGISTRY_USERNAME);
            if (dockerUsernameSetting != null && StringUtils.isNotBlank(dockerUsernameSetting.value())) {
                builder.serverId(String.format(SERVER_ID_TEMPLATE, dockerUsernameSetting.value()));
            }
            builder.os(OperatingSystemEnum.Docker);
        } else if (webapp.operatingSystem() == OperatingSystem.LINUX) {
            builder.os(OperatingSystemEnum.Linux);
            final RuntimeStack runtimeStack = AppServiceUtils.parseRuntimeStack(webapp.linuxFxVersion());
            builder.runtimeStack(runtimeStack);
        } else {
            builder.os(OperatingSystemEnum.Windows);
            final WebContainer webContainer = WebContainer.fromString(webapp.javaContainer() + " " + webapp.javaContainerVersion());
            builder.javaVersion(webapp.javaVersion());
            builder.webContainer(webContainer);
        }
        if (servicePlan != null) {
            builder.pricingTier(servicePlan.pricingTier());
            builder.servicePlanName(servicePlan.name());
            builder.servicePlanResourceGroup(servicePlan.resourceGroupName());
        }
        return builder.build();
    }

    private static String highlightDefaultValue(String defaultValue) {
        return StringUtils.isBlank(defaultValue) ? "" : String.format(" [%s]", TextUtils.blue(defaultValue));
    }

    private static String getDockerImageName(String linuxFxVersion) {
        String[] segments = linuxFxVersion.split(Pattern.quote("|"));
        if (segments.length != 2) {
            return null;
        }
        final String image = segments[1];
        if (!image.contains("/")) {
            return image;
        }
        segments = image.split(Pattern.quote("/"));
        return segments[segments.length - 1].trim();
    }

    private static boolean checkWebAppVisible(boolean isContainer, boolean isDockerOnly, boolean isJavaSEWebApp, boolean isDockerWebapp) {
        if (isDockerWebapp) {
            return true;
        }
        if (isDockerOnly) {
            return false;
        }
        if (isContainer) {
            return !isJavaSEWebApp;
        } else {
            return isJavaSEWebApp;
        }
    }

    private static String getWebAppTypeByPackaging(String packaging) {
        final boolean isContainer = !Utils.isJarPackagingProject(packaging);
        final boolean isDockerOnly = Utils.isPomPackagingProject(packaging);
        if (isDockerOnly) {
            return "Docker";
        } else if (isContainer) {
            return "Web Container";
        } else {
            return "Java SE";
        }
    }
}
