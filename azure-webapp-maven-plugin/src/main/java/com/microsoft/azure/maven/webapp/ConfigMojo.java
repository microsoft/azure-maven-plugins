/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.queryer.MavenPluginQueryer;
import com.microsoft.azure.maven.queryer.QueryFactory;
import com.microsoft.azure.maven.utils.CustomTextIoStringListReader;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.WebAppPomHandler;
import com.microsoft.azure.maven.webapp.models.WebAppOption;
import com.microsoft.azure.maven.webapp.parser.V2NoValidationConfigurationParser;
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.WebContainerUtils;
import com.microsoft.azure.maven.webapp.validator.V2ConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.DocumentException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final String PRICE_TIER_NOT_AVAIL = "The price tier \"%s\" is not available for current OS or runtime, use \"%s\" instead.";
    private static final String NO_JAVA_WEB_APPS = "There are no Java Web Apps in current subscription, please follow the following steps to create a new one.";
    private static final String LONG_LOADING_HINT = "It may take a few minutes to load all Java Web Apps, please be patient.";
    private static final String[] configTypes = { "Application", "Runtime", "DeploymentSlot" };
    private static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    private static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";
    private static final String SETTING_REGISTRY_USERNAME = "DOCKER_REGISTRY_SERVER_USERNAME";
    private static final String SERVER_ID_TEMPLATE = "Please add a server in Maven settings.xml related to username: %s and put the serverId here";

    private static final List<String> WEB_APP_PROPERTIES = Arrays.asList("resourceGroup", "appName", "runtime", "deployment", "region",
            "appServicePlanResourceGroup", "appServicePlanName", "deploymentSlot");

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

    private boolean isProjectConfigured() {
        final String pluginIdentifier = plugin.getPluginLookupKey();
        final Xpp3Dom configuration = MavenConfigUtils.getPluginConfiguration(getProject(), pluginIdentifier);

        if (configuration == null) {
            return false;
        }

        for (final Xpp3Dom child : configuration.getChildren()) {
            if (WEB_APP_PROPERTIES.contains(child.getName())) {
                return true;
            }
        }
        return false;
    }

    protected void config(WebAppConfiguration configuration) throws MojoFailureException, AzureExecutionException,
            IOException, IllegalAccessException {
        WebAppConfiguration result;
        do {
            if (configuration == null || !isProjectConfigured()) {
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
                result = updateConfiguration(configuration.toBuilder().build());
            }
        } while (!confirmConfiguration(result));
        Log.info(SAVING_TO_POM);
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
        if (configuration.getPricingTier() != null) {
            System.out.println("PricingTier : " + configuration.getPricingTier());
        }

        if (configuration.getOs() == null) {
            System.out.println(CONFIGURATION_NO_RUNTIME);
        } else {
            System.out.println("OS : " + configuration.getOs().toString());
            switch (configuration.getOs()) {
                case WINDOWS:
                case LINUX:
                    System.out.println("Java : " + JavaVersionUtils.formatJavaVersion(configuration.getJavaVersion()));
                    System.out.println("Web server stack: " + WebContainerUtils.formatWebContainer(configuration.getWebContainer()));
                    break;
                case DOCKER:
                    System.out.println("Image : " + configuration.getImage());
                    if (StringUtils.isNotBlank(configuration.getServerId())) {
                        System.out.println("ServerId : " + configuration.getServerId());
                    }
                    if (StringUtils.isNotBlank(configuration.getRegistryUrl())) {
                        System.out.println("RegistryUrl : " + configuration.getRegistryUrl());
                    }
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

    private WebAppConfiguration getDefaultConfiguration() {
        final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
        final String defaultName = getProject().getArtifactId() + "-" + System.currentTimeMillis();
        final String resourceGroup = defaultName + "-rg";
        final String defaultSchemaVersion = "v2";
        final Region defaultRegion = WebAppConfiguration.DEFAULT_REGION;

        return builder.appName(defaultName)
                .subscriptionId(subscriptionId)
                .resourceGroup(resourceGroup)
                .region(defaultRegion)
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
        final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = configuration.toBuilder();

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

        final boolean isJBoss = isJBossRuntime(configuration.getWebContainer());
        final PricingTier defaultPricingTierFromRuntime = isJBoss ?
            WebAppConfiguration.DEFAULT_JBOSS_PRICING_TIER : WebAppConfiguration.DEFAULT_PRICINGTIER;
        final String currentPricingTier = configuration.getPricingTier();
        final List<String> availablePriceList = getAvailablePricingTierList(configuration.getOs(), isJBoss);
        String defaultPricingTier = currentPricingTier;
        if (availablePriceList.stream().noneMatch(price -> StringUtils.equalsIgnoreCase(price, currentPricingTier))) {
            defaultPricingTier = AppServiceUtils.convertPricingTierToString(defaultPricingTierFromRuntime);
            if (StringUtils.isNotBlank(currentPricingTier)) {
                Log.warn(String.format(PRICE_TIER_NOT_AVAIL, currentPricingTier, defaultPricingTier));
            }
        }

        final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultPricingTier,
                availablePriceList, String.format(PRICING_TIER_PROMPT, defaultPricingTier));
        return builder
                .subscriptionId(subscriptionId)
                .appName(appName)
                .resourceGroup(resourceGroup)
                .region(Region.fromName(region))
                .pricingTier(pricingTier)
                .build();
    }

    private static boolean isJBossRuntime(WebContainer runtimeStack) {
        return runtimeStack != null && StringUtils.startsWithIgnoreCase(runtimeStack.getValue(), "JBOSSEAP");
    }

    private WebAppConfiguration getSlotConfiguration(WebAppConfiguration configuration)
            throws MojoFailureException {
        final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = configuration.toBuilder();

        final DeploymentSlotSetting deploymentSlotSetting = configuration.getDeploymentSlotSetting();
        final String defaultIsSlotDeploy = deploymentSlotSetting == null ? "N" : "Y";
        final String isSlotDeploy = queryer.assureInputFromUser("isSlotDeploy", defaultIsSlotDeploy, BOOLEAN_REGEX,
                "Deploy to slot?(Y/N)", null);
        if (StringUtils.equalsIgnoreCase(isSlotDeploy, "n")) {
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
        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = configuration.toBuilder();
        final OperatingSystem defaultOs = ObjectUtils.defaultIfNull(configuration.getOs(), OperatingSystem.LINUX);

        final String os = queryer.assureInputFromUser("OS", defaultOs, String.format("Define value for OS [%s]:", defaultOs));
        final OperatingSystem osEnu = OperatingSystem.fromString(os);
        WebContainer webContainer = null;
        builder.os(osEnu);
        switch (osEnu) {
            case LINUX:
            case WINDOWS:
                webContainer = getRuntimeConfigurationForWindowsOrLinux(osEnu, builder, configuration);
                break;
            case DOCKER:
                builder = getRuntimeConfigurationOfDocker(builder, configuration);
                break;
            default:
                throw new AzureExecutionException("The value of <os> is unknown.");
        }
        final boolean isJBoss = isJBossRuntime(webContainer);
        if (initial || pricingTierNotSupport(osEnu, configuration.getPricingTier(), isJBoss)) {
            final PricingTier defaultPricingTierEnu = isJBoss ? WebAppConfiguration.DEFAULT_JBOSS_PRICING_TIER : WebAppConfiguration.DEFAULT_PRICINGTIER;
            String defaultPricingTier = ObjectUtils.firstNonNull(configuration.getPricingTier(), defaultPricingTierEnu.getSize());
            final List<String> availablePriceList = getAvailablePricingTierList(osEnu, isJBoss);
            if (!availablePriceList.contains(defaultPricingTier)) {
                Log.warn(String.format("'%s' is not supported in '%s'", defaultPricingTier, os));
                defaultPricingTier = defaultPricingTierEnu.getSize();
            }
            final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultPricingTier, availablePriceList,
                String.format(PRICING_TIER_PROMPT, defaultPricingTier));
            builder.pricingTier(pricingTier);
        }
        return builder.build();
    }

    private static boolean pricingTierNotSupport(@Nonnull OperatingSystem parseOperationSystem, String pricingTier, boolean isJBoss) {
        if (StringUtils.isBlank(pricingTier)) {
            return false;
        }
        final PricingTier pricingTierEnum = PricingTier.fromString(pricingTier);
        if (pricingTierEnum == null) {
            return true;
        }
        final List<String> availablePriceList = getAvailablePricingTierList(parseOperationSystem, isJBoss);
        return Objects.isNull(findStringInCollectionIgnoreCase(availablePriceList, pricingTierEnum.getSize()));
    }

    private WebContainer getRuntimeConfigurationForWindowsOrLinux(OperatingSystem os,
        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder, WebAppConfiguration configuration) throws MojoFailureException {
        String defaultJavaVersion = JavaVersionUtils.formatJavaVersion(configuration.getJavaVersionOrDefault());
        final List<String> validJavaVersions = JavaVersionUtils.getValidJavaVersions();
        if (!validJavaVersions.contains(defaultJavaVersion)) {
            Log.warn(String.format("'%s' is not supported.", defaultJavaVersion));
            defaultJavaVersion = WebAppConfiguration.DEFAULT_JAVA_VERSION.toString();
        }

        final String javaVersionInput = queryer.assureInputFromUser(JAVA_VERSION, defaultJavaVersion,
                validJavaVersions, String.format(COMMON_PROMPT, JAVA_VERSION, defaultJavaVersion));
        final JavaVersion javaVersion = JavaVersionUtils.toAzureSdkJavaVersion(javaVersionInput);
        if (javaVersion == null) {
            throw new AzureToolkitRuntimeException(String.format("Cannot handle java version: '%s'", javaVersionInput));
        }
        // For project which package is jar, use java se runtime
        if (isJarProject()) {
            builder.javaVersion(javaVersion).webContainer(WebContainer.JAVA_SE);
            return WebContainer.JAVA_SE;
        }
        String webContainerOrDefault = configuration.getWebContainerOrDefault();
        final List<String> validWebContainers = WebContainerUtils.getAvailableWebContainer(os, javaVersion,
            Utils.isJarPackagingProject(this.project.getPackaging()));
        if (!validWebContainers.contains(webContainerOrDefault)) {
            Log.warn(String.format("'%s' is not supported.", webContainerOrDefault));
            webContainerOrDefault = WebAppConfiguration.DEFAULT_CONTAINER.toString();
        }
        final String webContainerInput = queryer.assureInputFromUser(WEB_CONTAINER, webContainerOrDefault,
            validWebContainers,
            String.format(COMMON_PROMPT, WEB_CONTAINER, webContainerOrDefault));
        final WebContainer webContainer = WebContainer.fromString(webContainerInput);
        builder.javaVersion(javaVersion).webContainer(webContainer);
        return webContainer;
    }

    private WebAppConfiguration.WebAppConfigurationBuilder<?, ?> getRuntimeConfigurationOfDocker(WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder,
                                                                        WebAppConfiguration configuration) throws MojoFailureException {
        final String image = queryer.assureInputFromUser("image", configuration.image, NOT_EMPTY_REGEX, null, null);
        final String serverId = queryer.assureInputFromUser("serverId", configuration.serverId, null, null, null);
        final String registryUrl = queryer.assureInputFromUser("registryUrl", configuration.registryUrl, null, null,
                null);
        return builder.image(image).serverId(serverId).registryUrl(registryUrl);
    }

    private static List<String> getAvailablePricingTierList(OperatingSystem operatingSystem, boolean isJBoss) {
        final Set<String> pricingTierSet = new HashSet<>();
        if (isJBoss) {
            pricingTierSet.add("P1v3");
            pricingTierSet.add("P2v3");
            pricingTierSet.add("P3v3");
            return new ArrayList<>(pricingTierSet);
        }
        // Linux and docker app service uses linux as the os of app service plan.
        final List<PricingTier> availablePricingTier = AppServiceUtils.getAvailablePricingTiers(operatingSystem);
        for (final PricingTier pricingTier : availablePricingTier) {
            pricingTierSet.add(pricingTier.getSize());
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
            throws AzureAuthFailureException {
        try {
            final AzureAppService az = getOrCreateAzureAppServiceClient();
            if (Objects.isNull(az)) {
                return null;
            }
            // get user selected sub id to persistent it in pom.xml
            this.subscriptionId = az.getDefaultSubscription().getId();
            // load configuration to detecting java or docker
            Log.info(LONG_LOADING_HINT);
            final List<WebAppOption> webAppOptionList = az.webapps().stream()
                    .map(WebAppOption::new).sorted().collect(Collectors.toList());

            // check empty: first time
            if (webAppOptionList.isEmpty()) {
                Log.warn(NO_JAVA_WEB_APPS);
                return null;
            }
            final boolean isContainer = !Utils.isJarPackagingProject(this.project.getPackaging());
            final boolean isDockerOnly = Utils.isPomPackagingProject(this.project.getPackaging());
            final List<WebAppOption> javaOrDockerWebapps = webAppOptionList.stream().filter(app -> app.isJavaWebApp() || app.isDockerWebapp())
                    .filter(app -> checkWebAppVisible(isContainer, isDockerOnly, app.isJavaSE(), app.isDockerWebapp())).sorted()
                    .collect(Collectors.toList());
            final TextIO textIO = TextIoFactory.getTextIO();
            final WebAppOption selectedApp = selectAzureWebApp(textIO, javaOrDockerWebapps,
                    getWebAppTypeByPackaging(this.project.getPackaging()), az.getDefaultSubscription());
            if (selectedApp == null || selectedApp.isCreateNew()) {
                return null;
            }

            final IWebApp webapp = az.webapp(selectedApp.getId());
            final String serverPlanId = selectedApp.getServicePlanId();
            IAppServicePlan servicePlan = null;
            if (StringUtils.isNotBlank(serverPlanId)) {
                servicePlan = az.appServicePlan(serverPlanId);
            }

            final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
            if (!AppServiceUtils.isDockerAppService(webapp)) {
                builder.resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources());
            }
            return getConfigurationFromExisting(webapp, servicePlan, builder);
        } catch (AzureToolkitAuthenticationException ex) {
            // if is valid for config goal to have error in authentication
            getLog().warn(String.format("Cannot authenticate due to error: %s, select existing webapp is skipped.", ex.getMessage()));
            return null;
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
        return new CustomTextIoStringListReader<WebAppOption>(textIO::getTextTerminal, null)
                .withCustomPrompt(String.format("Please choose a %s Web App%s: ", webAppType, highlightDefaultValue(WebAppOption.CREATE_NEW.toString())))
                .withNumberedPossibleValues(options).withDefaultValue(WebAppOption.CREATE_NEW)
                .read(String.format("%s Web Apps in subscription %s:", webAppType, TextUtils.blue(targetSubscription.getName())));
    }

    private static WebAppConfiguration getConfigurationFromExisting(IWebApp webapp, IAppServicePlan servicePlan,
                                                                    WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder) {
        // common configuration
        builder.appName(webapp.name())
                .resourceGroup(webapp.entity().getResourceGroup())
                .subscriptionId(Utils.getSubscriptionId(webapp.id()))
                .region(webapp.entity().getRegion());

        if (AppServiceUtils.isDockerAppService(webapp)) {
            builder.os(OperatingSystem.DOCKER);
            final Map<String, String> settings = webapp.entity().getAppSettings();

            final String imageSetting = settings.get(SETTING_DOCKER_IMAGE);
            if (StringUtils.isNotBlank(imageSetting)) {
                builder.image(imageSetting);
            } else {
                builder.image(webapp.entity().getDockerImageName());
            }
            final String registryServerSetting = settings.get(SETTING_REGISTRY_SERVER);
            if (StringUtils.isNotBlank(registryServerSetting)) {
                builder.registryUrl(registryServerSetting);
            }

            final String dockerUsernameSetting = settings.get(SETTING_REGISTRY_USERNAME);
            if (StringUtils.isNotBlank(dockerUsernameSetting)) {
                builder.serverId(String.format(SERVER_ID_TEMPLATE, dockerUsernameSetting));
            }
        } else {
            builder.os(webapp.getRuntime().getOperatingSystem());
            builder.javaVersion(webapp.getRuntime().getJavaVersion());
            builder.webContainer(webapp.getRuntime().getWebContainer());
        }
        if (servicePlan != null && servicePlan.entity() != null) {
            builder.pricingTier(AppServiceUtils.convertPricingTierToString(servicePlan.entity().getPricingTier()));
            builder.servicePlanName(servicePlan.name());
            builder.servicePlanResourceGroup(servicePlan.entity().getResourceGroup());
        }
        return builder.build();
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
