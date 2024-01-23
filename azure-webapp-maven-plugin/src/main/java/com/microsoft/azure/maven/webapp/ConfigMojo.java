/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.queryer.MavenPluginQueryer;
import com.microsoft.azure.maven.queryer.QueryFactory;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.SchemaVersion;
import com.microsoft.azure.maven.webapp.handlers.WebAppPomHandler;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppDockerRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.fromAppService;

/**
 * Generate configuration for web app maven plugin or init the configuration from existing app service instance.
 */
@Slf4j
@Mojo(name = "config")
public class ConfigMojo extends AbstractWebAppMojo {
    private static final String WEB_CONTAINER = "webContainer";
    private static final String JAVA_VERSION = "javaVersion";
    private static final String COMMON_PROMPT = "Define value for %s [%s]:";
    private static final String PRICING_TIER_PROMPT = "Define value for pricingTier [%s]:";
    private static final String NOT_EMPTY_REGEX = "[\\s\\S]+";
    private static final String BOOLEAN_REGEX = "[YyNn]";
    public static final String SUBSCRIPTION_ID_PATTERN = "[a-fA-F0-9\\-]{30,36}";
    public static final String APP_NAME_PATTERN = "[a-zA-Z0-9\\-]{2,60}";
    public static final String RESOURCE_GROUP_PATTERN = "[a-zA-Z0-9\\.\\_\\-\\(\\)]{1,90}";
    public static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";

    private static final String CONFIG_ONLY_SUPPORT_V2 = "Config only support V2 schema";
    private static final String CHANGE_OS_WARNING = "The plugin may not work if you change the os of an existing webapp.";
    private static final String CONFIGURATION_NO_RUNTIME = "No runtime configuration, skip it.";
    private static final String SAVING_TO_POM = "Saving configuration to pom.";

    private static final String PRICE_TIER_NOT_AVAIL = "The price tier \"%s\" is not available for current OS or runtime, use \"%s\" instead.";
    private static final String NO_JAVA_WEB_APPS = "There are no Java Web Apps in current subscription, please follow the following steps to create a new one.";
    private static final String LONG_LOADING_HINT = "It may take a few minutes to load all Java Web Apps, please be patient.";
    private static final String[] configTypes = {"Application", "Runtime", "DeploymentSlot"};
    private static final String SETTING_REGISTRY_USERNAME = "DOCKER_REGISTRY_SERVER_USERNAME";
    private static final String SERVER_ID_TEMPLATE = "Please add a server in Maven settings.xml related to username: %s and put the serverId here";

    private static final List<String> WEB_APP_PROPERTIES = Arrays.asList("resourceGroup", "appName", "runtime", "deployment", "region",
        "appServicePlanResourceGroup", "appServicePlanName", "deploymentSlot");
    public static final String WEB_APP_STACKS_API = "https://aka.ms/maven_webapp_runtime";
    private MavenPluginQueryer queryer;
    private WebAppPomHandler pomHandler;

    @Override
    @AzureOperation(name = "user/webapp.config")
    protected void doExecute() {
        if (!(Utils.isJarPackagingProject(this.project.getPackaging()) ||
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
                getWebAppConfiguration();
            if (!isV2Configuration(configuration)) {
                log.warn(CONFIG_ONLY_SUPPORT_V2);
            } else {
                config(configuration);
            }
        } catch (final DocumentException | MojoFailureException | IOException | IllegalAccessException | AzureExecutionException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
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

    protected void config(WebAppConfiguration configuration) throws MojoFailureException, IOException, IllegalAccessException, DocumentException {
        WebAppConfiguration result;
        do {
            if (configuration == null || !isProjectConfigured()) {
                try {
                    final String createNewConfiguration =
                        queryer.assureInputFromUser("confirm", "Y", BOOLEAN_REGEX, "Create new run configuration (Y/N)", null);
                    if (StringUtils.equalsIgnoreCase(createNewConfiguration, "Y")) {
                        result = initConfig();
                    } else {
                        result = Optional.ofNullable(chooseExistingWebappForConfiguration()).orElseGet(this::initConfig);
                    }
                } catch (final AzureAuthFailureException e) {
                    throw new AzureToolkitRuntimeException(String.format("Cannot get Web App list due to error: %s.", e.getMessage()), e);
                }
            } else {
                result = updateConfiguration(configuration.toBuilder().build());
            }
        } while (!confirmConfiguration(result));
        log.info(SAVING_TO_POM);
        pomHandler.updatePluginConfiguration(result, configuration, this.project, plugin);
    }

    protected boolean confirmConfiguration(WebAppConfiguration configuration) {
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
            System.out.println("OS : " + configuration.getOs());
            switch (configuration.getOs()) {
                case WINDOWS:
                case LINUX:
                    System.out.println("Java Version: " + configuration.getJavaVersion());
                    System.out.println("Web server stack: " + configuration.getWebContainer());
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
                    throw new AzureToolkitRuntimeException("The value of <os> is unknown.");
            }
        }
        System.out.println("Deploy to slot : " + (configuration.getDeploymentSlotSetting() != null));
        if (configuration.getDeploymentSlotSetting() != null) {
            final DeploymentSlotSetting slotSetting = configuration.getDeploymentSlotSetting();
            System.out.println("Slot name : " + slotSetting.getName());
            System.out.println("ConfigurationSource : " + slotSetting.getConfigurationSource());
        }
        final String result = queryer.assureInputFromUser("confirm", "Y", BOOLEAN_REGEX, "Confirm (Y/N)", null);
        return "Y".equalsIgnoreCase(result);
    }

    protected WebAppConfiguration initConfig() {
        final WebAppConfiguration result = getDefaultConfiguration();
        return setRuntimeConfiguration(result, false);
    }

    private WebAppConfiguration getDefaultConfiguration() {
        final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
        final String defaultName = getProject().getArtifactId() + "-" + System.currentTimeMillis();
        final String resourceGroup = defaultName + "-rg";
        final String defaultSchemaVersion = "v2";
        final Region defaultRegion = WebAppConfiguration.getDefaultRegion();

        return builder.appName(defaultName)
            .subscriptionId(subscriptionId)
            .resourceGroup(resourceGroup)
            .region(defaultRegion)
            .resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources())
            .schemaVersion(defaultSchemaVersion)
            .subscriptionId(this.subscriptionId)
            .build();
    }

    protected WebAppConfiguration updateConfiguration(WebAppConfiguration configuration) {
        final String selection = queryer.assureInputFromUser("selection", configTypes[0], Arrays.asList(configTypes),
            String.format("Please choose which part to config [%s]:", configTypes[0]));
        switch (selection) {
            case "Application":
                return getWebAppConfiguration(configuration);
            case "Runtime":
                log.warn(CHANGE_OS_WARNING);
                return setRuntimeConfiguration(configuration, true);
            case "DeploymentSlot":
                return getSlotConfiguration(configuration);
            default:
                throw new AzureToolkitRuntimeException("Unknown webapp setting");
        }
    }

    private WebAppConfiguration getWebAppConfiguration(WebAppConfiguration configuration) {
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
        final String region = queryer.assureInputFromUser("region", defaultRegion, NOT_EMPTY_REGEX, null, null);

        PricingTier defaultTier = PricingTier.fromString(configuration.getPricingTier());
        if (Objects.isNull(defaultTier)) {
            defaultTier = isJBossRuntime(configuration.getWebContainer()) ? WebAppConfiguration.DEFAULT_JBOSS_PRICING_TIER : WebAppConfiguration.DEFAULT_PRICINGTIER;
        }
        final List<PricingTier> validTiers = new ArrayList<>(WebAppRuntime.getPricingTiers(configuration.getOs(), configuration.getWebContainer()));
        final List<String> validTierTexts = validTiers.stream().map(PricingTier::getSize).collect(Collectors.toList());
        if (!validTiers.contains(defaultTier)) {
            log.warn(String.format("'%s' is not supported for runtime('%s')", defaultTier.getSize(), configuration.getWebContainer()));
            defaultTier = validTiers.contains(PricingTier.PREMIUM_P1V2) ? PricingTier.PREMIUM_P1V2 : validTiers.get(0);
        }
        final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultTier.getSize(), validTierTexts,
            String.format(PRICING_TIER_PROMPT, defaultTier.getSize()));
        return builder
            .subscriptionId(subscriptionId)
            .appName(appName)
            .resourceGroup(resourceGroup)
            .region(Region.fromName(region))
            .pricingTier(pricingTier)
            .build();
    }

    private static boolean isJBossRuntime(String container) {
        return container != null && StringUtils.startsWithIgnoreCase(container, "JBOSS");
    }

    private WebAppConfiguration getSlotConfiguration(WebAppConfiguration configuration) {
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
        final String slotName = queryer.assureInputFromUser("slotName", defaultSlotName, SLOT_NAME_PATTERN,
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

    private WebAppConfiguration setRuntimeConfiguration(WebAppConfiguration configuration, boolean updating) {
        final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = configuration.toBuilder();
        final OperatingSystem defaultOs = ObjectUtils.defaultIfNull(configuration.getOs(), OperatingSystem.LINUX);

        final String osUserInput = queryer.assureInputFromUser("OS", defaultOs, String.format("Define value for OS [%s]:", defaultOs));
        final OperatingSystem os = OperatingSystem.fromString(osUserInput);
        Optional.ofNullable(os).orElseThrow(() -> new AzureToolkitRuntimeException("The value of <os> is unknown."));
        builder.os(os);
        final WebAppRuntime runtime = os == OperatingSystem.DOCKER ? setupRuntimeForDocker(builder, configuration) : setupRuntimeForWindowsOrLinux(os, builder, configuration, updating);
        if (!updating || pricingTierNotSupport(configuration.getPricingTier(), runtime)) {
            PricingTier defaultTier = runtime.isJBoss() ? WebAppConfiguration.DEFAULT_JBOSS_PRICING_TIER : WebAppConfiguration.DEFAULT_PRICINGTIER;
            final List<PricingTier> validTiers = runtime.getPricingTiers();
            final List<String> validTierTexts = validTiers.stream().map(PricingTier::getSize).collect(Collectors.toList());
            if (!validTiers.contains(defaultTier)) {
                log.warn(String.format("'%s' is not supported in current os('%s') and runtime('%s')", defaultTier.getSize(), osUserInput, runtime.getContainerUserText()));
                defaultTier = validTiers.contains(PricingTier.PREMIUM_P1V2) ? PricingTier.PREMIUM_P1V2 : validTiers.get(0);
            }
            final String pricingTier = queryer.assureInputFromUser("pricingTier", defaultTier.getSize(), validTierTexts,
                String.format(PRICING_TIER_PROMPT, defaultTier.getSize()));
            builder.pricingTier(pricingTier);
        }
        return builder.build();
    }

    private static boolean pricingTierNotSupport(String pricingTier, WebAppRuntime runtime) {
        if (StringUtils.isBlank(pricingTier)) {
            return false;
        }
        final PricingTier tier = PricingTier.fromString(pricingTier);
        if (tier == null) {
            return true;
        }
        final List<PricingTier> tiers = runtime.getPricingTiers();
        return !tiers.contains(tier);
    }

    private WebAppRuntime setupRuntimeForWindowsOrLinux(OperatingSystem os,
                                                        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder,
                                                        WebAppConfiguration configuration, final boolean updating) {
        // filter runtimes by os
        List<WebAppRuntime> runtimes = WebAppRuntime.getMajorRuntimes().stream()
            .filter(runtime -> runtime.getOperatingSystem() == os)
            .collect(Collectors.toList());

        // filter runtimes by java version (java target/release level)
        runtimes = getValidRuntimes(runtimes, WebAppRuntime::getJavaMajorVersionNumber, WebAppRuntime::getJavaVersionUserText);

        final List<String> validJavaVersionUserTexts = runtimes.stream().map(WebAppRuntime::getJavaVersionUserText).distinct().collect(Collectors.toList());
        String defaultJavaVersion = configuration.getJavaVersion();
        if (StringUtils.isNotBlank(defaultJavaVersion)) {
            defaultJavaVersion = StringUtils.startsWithIgnoreCase(defaultJavaVersion, "java") ? StringUtils.capitalize(defaultJavaVersion) : String.format("Java %s", defaultJavaVersion);
        }
        if (StringUtils.isBlank(defaultJavaVersion) || !validJavaVersionUserTexts.contains(defaultJavaVersion)) {
            if (updating && StringUtils.isNotBlank(defaultJavaVersion)) {
                log.warn(String.format("'%s' may not be a valid Java runtime. Refer to %s please.", defaultJavaVersion, WEB_APP_STACKS_API));
            }
            if (validJavaVersionUserTexts.isEmpty()) {
                throw new AzureToolkitRuntimeException("No valid runtime found, please check your configuration and try again.");
            }
            defaultJavaVersion = validJavaVersionUserTexts.get(0);
        }

        // ask user to select java version
        final String javaVersionUserInput = queryer.assureInputFromUser(JAVA_VERSION, defaultJavaVersion,
            validJavaVersionUserTexts, String.format(COMMON_PROMPT, JAVA_VERSION, defaultJavaVersion));
        if (!validJavaVersionUserTexts.contains(javaVersionUserInput)) {
            final String message = String.format("'%s' may not be a valid Java runtime, recommended values are %s. Refer to %s please.", javaVersionUserInput, String.join(", ", validJavaVersionUserTexts), WEB_APP_STACKS_API);
            throw new AzureToolkitRuntimeException(message);
        }
        builder.javaVersion(javaVersionUserInput);
        final boolean isJarPackaging = Utils.isJarPackagingProject(project.getPackaging());
        if (isJarPackaging) {
            log.info("Skip web container selection for \"jar\" project.");
            builder.webContainer(WebAppRuntime.JAVA_SE.toString());
            runtimes = runtimes.stream().filter(r -> StringUtils.equalsIgnoreCase(r.getContainerUserText(), WebAppRuntime.JAVA_SE.toString())).collect(Collectors.toList());
            if (runtimes.isEmpty()) {
                throw new AzureToolkitRuntimeException("No valid runtime found, please check your configuration and try again.");
            }
            return runtimes.get(0);
        }

        // filter runtimes by java version (user selection)
        runtimes = runtimes.stream().filter(r -> StringUtils.equalsIgnoreCase(r.getJavaVersionUserText(), javaVersionUserInput)).collect(Collectors.toList());

        // filter runtimes by web container (packaging)
        runtimes = runtimes.stream().filter(r -> !StringUtils.equalsIgnoreCase(r.getContainerName(), "java")).collect(Collectors.toList());
        if (Utils.isEarPackagingProject(project.getPackaging())) {
            runtimes = runtimes.stream().filter(r -> StringUtils.startsWithIgnoreCase(r.getContainerName(), "JBoss")).collect(Collectors.toList());
        }

        final List<String> validContainerUserTexts = runtimes.stream().map(WebAppRuntime::getContainerUserText).distinct().collect(Collectors.toList());
        String defaultContainer = configuration.getWebContainer();
        if (StringUtils.isBlank(defaultContainer) || !validContainerUserTexts.contains(defaultContainer)) {
            if (updating && StringUtils.isNotBlank(defaultContainer)) {
                log.warn(String.format("'%s' may not be a valid web container. Refer to %s please.", defaultContainer, WEB_APP_STACKS_API));
            }
            if (validContainerUserTexts.isEmpty()) {
                final String message = String.format("No valid runtime found for '%s' + '%s', please check your java version and try again.", os, javaVersionUserInput);
                throw new AzureToolkitRuntimeException(message);
            }
            defaultContainer = validContainerUserTexts.get(0);
        }

        final String containerUserInput = queryer.assureInputFromUser(WEB_CONTAINER, defaultContainer,
            validContainerUserTexts,
            String.format(COMMON_PROMPT, WEB_CONTAINER, defaultContainer));
        if (!validContainerUserTexts.contains(containerUserInput)) {
            final String message = String.format("'%s' may not be a valid web container, recommended values are %s. Refer to %s please", containerUserInput, String.join(", ", validContainerUserTexts), WEB_APP_STACKS_API);
            throw new AzureToolkitRuntimeException(message);
        }
        builder.webContainer(containerUserInput);

        // filter runtimes by java version (user selection)
        runtimes = runtimes.stream().filter(r -> StringUtils.equalsIgnoreCase(r.getContainerUserText(), containerUserInput)).collect(Collectors.toList());
        if (runtimes.isEmpty()) {
            final String message = String.format("No valid runtime found for '%s' + '%s' + '%s', please check your configuration and try again.", os, javaVersionUserInput, containerUserInput);
            throw new AzureToolkitRuntimeException(message);
        }
        return runtimes.get(0);
    }

    private WebAppDockerRuntime setupRuntimeForDocker(
        WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder, WebAppConfiguration configuration) {
        final String image = queryer.assureInputFromUser("image", configuration.image, NOT_EMPTY_REGEX, null, null);
        final String serverId = queryer.assureInputFromUser("serverId", configuration.serverId, null, null, null);
        final String registryUrl = queryer.assureInputFromUser("registryUrl", configuration.registryUrl, null, null,
            null);
        builder.image(image).serverId(serverId).registryUrl(registryUrl);
        return WebAppDockerRuntime.INSTANCE;
    }

    private String getDefaultValue(String defaultValue, String fallBack, String pattern) {
        return StringUtils.isNotEmpty(defaultValue) && defaultValue.matches(pattern) ? defaultValue : fallBack;
    }

    private WebAppConfiguration getWebAppConfiguration() throws AzureExecutionException {
        validateConfiguration(message -> AzureMessager.getMessager().warning(message.getMessage()), false);
        return getConfigParser().getWebAppConfiguration();
    }

    private WebAppConfiguration chooseExistingWebappForConfiguration()
        throws AzureAuthFailureException {
        try {
            final AzureAppService az = initAzureAppServiceClient();
            if (Objects.isNull(az)) {
                return null;
            }
            log.info(LONG_LOADING_HINT);
            WebAppRuntime.tryLoadingAllRuntimes();
            final List<WebApp> apps = az.list().stream().flatMap(m -> m.webApps().list().stream()).parallel()
                .filter(webApp -> webApp.getRuntime() != null)
                .filter(webApp -> {
                    final WebAppRuntime runtime = webApp.getRuntime();
                    if (Utils.isJarPackagingProject(this.project.getPackaging())) {
                        return runtime.isJavaSE();
                    } else if (Utils.isWarPackagingProject(this.project.getPackaging())) {
                        return runtime.isTomcat() || runtime.isJBoss();
                    } else if (Utils.isEarPackagingProject(this.project.getPackaging())) {
                        return runtime.isJBoss();
                    }
                    return runtime.isDocker();
                }).collect(Collectors.toList());

            // check empty: first time
            if (apps.isEmpty()) {
                log.warn(NO_JAVA_WEB_APPS);
                return null;
            }
            // ask user to select a webapp
            final List<String> appNames = apps.stream().map(app -> String.format("%s (%s)", app.getName(), app.getRuntime().getDisplayName())).collect(Collectors.toList());
            final String appText = queryer.assureInputFromUser("webApp", appNames.get(0),
                appNames, String.format("select a Azure Web App: [%s]", appNames.get(0)));

            final String[] parts = appText.split(" \\(", 2);
            final WebApp webapp = apps.stream().filter(app -> app.getName().equalsIgnoreCase(parts[0].trim())).findFirst().orElse(null);
            if (webapp == null) {
                return null;
            }
            this.subscriptionId = webapp.getSubscriptionId();
            final WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder = WebAppConfiguration.builder();
            if (!AppServiceUtils.isDockerAppService(webapp)) {
                builder.resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources());
            }
            return getConfigurationFromExisting(webapp, builder);
        } catch (final AzureToolkitAuthenticationException ex) {
            // if is valid for config goal to have error in authentication
            getLog().warn(String.format("Cannot authenticate due to error: %s, select existing webapp is skipped.", ex.getMessage()));
            return null;
        }
    }

    private WebAppConfiguration getConfigurationFromExisting(WebApp webapp,
                                                             WebAppConfiguration.WebAppConfigurationBuilder<?, ?> builder) {
        final AppServiceConfig appServiceConfig = fromAppService(webapp, webapp.getAppServicePlan());
        // common configuration
        builder.appName(appServiceConfig.appName())
            .resourceGroup(appServiceConfig.resourceGroup())
            .subscriptionId(appServiceConfig.subscriptionId())
            .region(appServiceConfig.region());
        builder.os(appServiceConfig.runtime().os());
        if (AppServiceUtils.isDockerAppService(webapp)) {
            final Map<String, String> settings = webapp.getAppSettings();
            builder.image(appServiceConfig.runtime().image());
            builder.registryUrl(appServiceConfig.runtime().registryUrl());
            final String dockerUsernameSetting = settings.get(SETTING_REGISTRY_USERNAME);
            if (StringUtils.isNotBlank(dockerUsernameSetting)) {
                builder.serverId(String.format(SERVER_ID_TEMPLATE, dockerUsernameSetting));
            }
        } else {
            final WebAppRuntime runtime = webapp.getRuntime();
            if (runtime != null) {
                builder.webContainer(runtime.getContainerUserText());
                builder.javaVersion(runtime.getJavaVersionUserText());
            }
        }
        builder.servicePlanName(appServiceConfig.servicePlanName());
        builder.servicePlanResourceGroup(appServiceConfig.servicePlanResourceGroup());
        builder.pricingTier(Objects.toString(appServiceConfig.pricingTier()));
        return builder.build();
    }
}
