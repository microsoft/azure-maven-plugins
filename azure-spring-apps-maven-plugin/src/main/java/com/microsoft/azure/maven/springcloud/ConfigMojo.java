/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.azure.core.management.Region;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.springcloud.config.AppDeploymentRawConfig;
import com.microsoft.azure.maven.springcloud.config.AppRawConfig;
import com.microsoft.azure.maven.springcloud.config.ClusterRawConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationPrompter;
import com.microsoft.azure.maven.springcloud.config.ConfigurationUpdater;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterModule;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.model.Sku;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.atteo.evo.inflector.English;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.DocumentException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate configuration for spring apps maven plugin or init the configuration from existing Azure Spring app instance.
 */
@Slf4j
@Mojo(name = "config", requiresDirectInvocation = true, aggregator = true)
public class ConfigMojo extends AbstractMojoBase {
    private static final String DEPLOYMENT_TAG = "deployment";
    private static final List<String> APP_PROPERTIES = Arrays.asList("appName", "isPublic");
    private static final List<String> DEPLOYMENT_PROPERTIES = Arrays.asList("cpu", "memoryInGB", "instanceCount", "jvmOptions", "runtimeVersion");

    private static final String JAVA_8 = "Java 8";
    private static final String JAVA_11 = "Java 11";
    private static final String JAVA_17 = "Java 17";
    private static final Map<String, Integer> JAVA_RUNTIMES = new LinkedHashMap<String, Integer>() {
        {
            put(JAVA_8, 8);
            put(JAVA_11, 11);
            put(JAVA_17, 17);
        }
    };

    private Boolean parentMode; // this is not a mojo parameter

    /**
     * The prompt wrapper to get user input for each property.
     */
    private ConfigurationPrompter wrapper;

    /**
     * The list of configure-able projects under current parent folder(selected by user).
     */
    private List<MavenProject> targetProjects = new ArrayList<>();

    /**
     * The list of all public projects specified by user.
     */
    private List<MavenProject> publicProjects;

    /**
     * The map of project to the default appName.
     */
    private Map<MavenProject, String> appNameByProject;

    /**
     * The app settings collected from user.
     */
    private AppRawConfig appSettings;

    /**
     * The cluster settings collected from user.
     */
    private ClusterRawConfig clusterSettings;

    /**
     * The deployment settings collected from user.
     */
    private AppDeploymentRawConfig deploymentSettings;

    /**
     * The maven variable used to evaluate maven expression.
     */
    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    /**
     * Boolean flag to control whether to prompt the advanced options
     */
    @Parameter(property = "advancedOptions")
    private Boolean advancedOptions;

    private boolean useExistingCluster = false;

    @Override
    @AzureOperation("user/springcloud.config_mojo")
    protected void doExecute() throws AzureExecutionException {
        if (!settings.isInteractiveMode()) {
            throw new UnsupportedOperationException("The goal 'config' must be run at interactive mode.");
        }

        if (!MavenConfigUtils.isPomPackaging(this.project) && !MavenConfigUtils.isJarPackaging(this.project)) {
            throw new UnsupportedOperationException(
                    String.format("The project (%s) with packaging %s is not supported for Azure Spring Apps.", this.project.getName(),
                            this.project.getPackaging()));
        }
        if (isProjectConfigured(this.project)) {
            log.warn(String.format("Project (%s) is already configured and won't be affected by this command.", this.project.getName()));
            return;
        }
        appSettings = new AppRawConfig();
        clusterSettings = new ClusterRawConfig();
        deploymentSettings = new AppDeploymentRawConfig();
        parentMode = MavenConfigUtils.isPomPackaging(this.project);
        if (parentMode && BooleanUtils.isTrue(advancedOptions)) {
            throw new UnsupportedOperationException("The \"advancedOptions\" mode is not supported at parent folder.");
        }
        final ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        try {
            this.wrapper = new ConfigurationPrompter(expressionEvaluator);
            this.wrapper.initialize();
            this.wrapper.putCommonVariable("project", this.project);

            selectProjects();
            if (targetProjects == null || targetProjects.isEmpty()) {
                // no need to proceed when there are no projects need to be configured
                return;
            }
            // select subscription in spring apps -> config is different from other goals since it is prompted after select project.
            // set up account and select subscription here
            loginAzure();
            promptAndSelectSubscription();

            // prompt to select existing cluster or create a new one
            useExistingCluster = this.wrapper.handleConfirm("Using existing Azure Spring Apps in Azure (Y/n):", true, true);
            final SpringCloudCluster cluster = useExistingCluster ? selectAppCluster() : configCluster();
            // todo: handle empty cluster cases
            final boolean useExistingApp = Objects.nonNull(cluster) && !parentMode &&
                this.wrapper.handleConfirm(String.format("Using existing app in Azure Spring Apps %s (y/N):", cluster.getName()), false, true);
            if (useExistingApp) {
                selectApp(cluster);
            } else {
                configCommon();
            }
            confirmAndSave();
        } catch (IOException | InvalidConfigurationException | UnsupportedOperationException | MavenDecryptException | AzureToolkitAuthenticationException e) {
            throw new AzureExecutionException(e.getMessage());
        } finally {
            if (this.wrapper != null) {
                try {
                    this.wrapper.close();
                } catch (IOException e) {
                    // ignore at final step
                }
            }

        }
    }

    private SpringCloudCluster configCluster() throws IOException, InvalidConfigurationException {
        configureClusterName();
        configureResourceGroup();
        final Sku sku = configureSku();
        configureRegion(sku);
        if (sku.isConsumptionTier()) {
            configureEnvironment();
        }
        return null;
    }

    private void configureClusterName() throws IOException, InvalidConfigurationException {
        final String cluster = this.wrapper.handle("configure-cluster-name", false);
        this.clusterSettings.setClusterName(cluster);
        this.wrapper.putCommonVariable("cluster", cluster);
    }

    private void configureResourceGroup() throws IOException, InvalidConfigurationException {
        final String resourceGroup = this.wrapper.handle("configure-resource-group-name", false);
        this.clusterSettings.setResourceGroup(resourceGroup);
    }

    private Region configureRegion(@Nonnull final Sku sku) throws IOException, InvalidConfigurationException {
        final List<Region> regions = Azure.az(AzureSpringCloud.class)
            .forSubscription(getSubscriptionId()).listSupportedRegions(sku);
        assert CollectionUtils.isNotEmpty(regions) : String.format("No valid region found for sku %s.", sku.toString());
        this.wrapper.putCommonVariable("regions", regions);
        // todo: improve the logic to get default region
        final Region defaultRegion = regions.contains(Region.US_EAST) ? Region.US_EAST : regions.get(0);
        final Region result = autoUseDefault() ? defaultRegion : this.wrapper.handleSelectOne("configure-region", regions, defaultRegion, Region::name);
        this.clusterSettings.setRegion(result.name());
        return result;
    }

    private Sku configureSku() throws IOException, InvalidConfigurationException {
        final List<Sku> skus = Azure.az(AzureSpringCloud.class)
            .forSubscription(getSubscriptionId()).listSupportedSkus(null);
        assert CollectionUtils.isNotEmpty(skus) : "No valid sku found in current subscription.";
        this.wrapper.putCommonVariable("skus", skus);
        final Sku defaultSku = skus.contains(Sku.SPRING_APPS_DEFAULT_SKU) ? Sku.SPRING_APPS_DEFAULT_SKU : skus.get(0);
        final Sku result = autoUseDefault() ? defaultSku : this.wrapper.handleSelectOne("configure-sku", skus, defaultSku, Sku::toString);
        this.clusterSettings.setSku(result.toString());
        return result;
    }

    private void configureEnvironment() throws IOException, InvalidConfigurationException {
        // todo: support create new environment
        final List<ContainerAppsEnvironment> environments = Azure.az(AzureContainerApps.class)
            .environments(getSubscriptionId()).list();
        if (CollectionUtils.isEmpty(environments)) {
            final String defaultEnvironment = String.format("cae-%s", this.clusterSettings.getClusterName());
            this.clusterSettings.setEnvironment(defaultEnvironment);
            log.info(String.format("No environment found, will create new app environment %s.", defaultEnvironment));
        }
        this.wrapper.putCommonVariable("environments", environments);
        final ContainerAppsEnvironment defaultEnvironment = environments.get(0);
        final ContainerAppsEnvironment containerAppsEnvironment = autoUseDefault() ? defaultEnvironment :
            this.wrapper.handleSelectOne("configure-environment", environments, defaultEnvironment, ContainerAppsEnvironment::getName);
        this.clusterSettings.setEnvironment(containerAppsEnvironment.getName());
        this.clusterSettings.setEnvironmentResourceGroup(containerAppsEnvironment.getResourceGroupName());
    }

    private void configCommon() throws IOException, InvalidConfigurationException {
        configureAppName();
        if (this.notEnterpriseTier()) {
            configureJavaVersion();
        }
        configurePublic();
        configDeployment();
    }

    private void configDeployment() throws IOException, InvalidConfigurationException {
        configureInstanceCount();
        configureCpu();
        configureMemory();
        configureJvmOptions();
    }

    private boolean notEnterpriseTier() {
        return !Sku.fromString(this.clusterSettings.getSku()).isEnterpriseTier();
    }

    private void selectProjects() throws IOException, InvalidConfigurationException {
        if (this.parentMode) {
            final List<MavenProject> allProjects = session.getAllProjects().stream().filter(MavenConfigUtils::isJarPackaging)
                .collect(Collectors.toList());
            final List<MavenProject> configuredProjects = new ArrayList<>();
            for (final MavenProject proj : allProjects) {
                if (isProjectConfigured(proj)) {
                    configuredProjects.add(proj);
                } else {
                    targetProjects.add(proj);
                }
            }
            this.wrapper.putCommonVariable("projects", targetProjects);
            if (!configuredProjects.isEmpty()) {
                log.warn(String.format("The following child %s %s already configured: ", English.plural("module", configuredProjects.size()),
                    configuredProjects.size() > 1 ? "are" : "is"));
                for (final MavenProject proj : configuredProjects) {
                    System.out.println("    " + TextUtils.yellow(proj.getName()));
                }
            } else if (targetProjects.isEmpty()) {
                log.warn("There are no projects in current folder with package 'jar'.");
                return;
            }

            targetProjects = this.wrapper.handleMultipleCase("select-projects", targetProjects, MavenProject::getName);
        } else {
            // in single project mode, add the current project to targetProjects
            targetProjects.add(this.project);
        }
        this.wrapper.putCommonVariable("projects", targetProjects);

    }

    private void configureJavaVersion() throws IOException, InvalidConfigurationException {
        final List<String> validRuntimes = getValidRuntimes(new ArrayList<>(JAVA_RUNTIMES.keySet()), JAVA_RUNTIMES::get);
        assert CollectionUtils.isNotEmpty(validRuntimes) : "No valid runtime found for current project.";
        this.wrapper.putCommonVariable("runtimes", validRuntimes);
        final String defaultRuntime = validRuntimes.contains(JAVA_11) ? JAVA_11 : validRuntimes.get(0);
        this.deploymentSettings.setRuntimeVersion(this.wrapper.handleSelectOne("configure-java-version", validRuntimes, defaultRuntime, t -> t));
    }

    private void configureJvmOptions() throws IOException, InvalidConfigurationException {
        this.deploymentSettings.setJvmOptions(this.wrapper.handle("configure-jvm-options", autoUseDefault()));
    }

    private void configureCpu() throws IOException, InvalidConfigurationException {
        this.deploymentSettings.setCpu(this.wrapper.handle("configure-cpu", autoUseDefault()));
    }

    private void configureMemory() throws IOException, InvalidConfigurationException {
        this.deploymentSettings.setMemoryInGB(this.wrapper.handle("configure-memory", autoUseDefault()));
    }

    private void configureInstanceCount() throws IOException, InvalidConfigurationException {
        this.deploymentSettings.setInstanceCount(this.wrapper.handle("configure-instance-count", autoUseDefault()));
    }

    private boolean autoUseDefault() {
        return !BooleanUtils.isTrue(advancedOptions) || parentMode;
    }

    private void confirmAndSave() throws IOException {
        final Map<String, String> changesToConfirm = new LinkedHashMap<>();
        changesToConfirm.put("Subscription id", this.subscriptionId);
        changesToConfirm.put("Resource group name", this.clusterSettings.getResourceGroup());
        changesToConfirm.put("Azure Spring Apps name", this.clusterSettings.getClusterName());
        if (this.notEnterpriseTier()) {
            changesToConfirm.put("Runtime Java version", this.deploymentSettings.getRuntimeVersion());
        }
        if (!useExistingCluster) {
            changesToConfirm.put("Region", this.clusterSettings.getRegion());
            changesToConfirm.put("Sku", this.clusterSettings.getSku());
            if (StringUtils.equalsIgnoreCase("StandardGen2", this.clusterSettings.getSku())) {
                changesToConfirm.put("Environment", this.clusterSettings.getEnvironment());
                changesToConfirm.put("Environment resource group", this.clusterSettings.getEnvironmentResourceGroup());
            }
        }
        if (this.parentMode) {
            changesToConfirm.put("App " + English.plural("name", this.appNameByProject.size()),
                String.join(",", appNameByProject.values()));
            if (this.publicProjects != null && this.publicProjects.size() > 0) {
                changesToConfirm.put("Public " + English.plural("app", this.publicProjects.size()),
                        publicProjects.stream().map(p -> appNameByProject.get(p)).collect(Collectors.joining(",")));
            }
            this.wrapper.confirmChanges(changesToConfirm, this::saveConfigurationToPom);
        } else {
            changesToConfirm.put("App name", this.appSettings.getAppName());
            changesToConfirm.put("Public access", this.appSettings.getIsPublic());
            changesToConfirm.put("Instance count/max replicas", this.deploymentSettings.getInstanceCount());
            changesToConfirm.put("CPU count", this.deploymentSettings.getCpu());
            changesToConfirm.put("Memory size(GB)", this.deploymentSettings.getMemoryInGB());
            changesToConfirm.put("JVM options", this.deploymentSettings.getJvmOptions());
            this.wrapper.confirmChanges(changesToConfirm, this::saveConfigurationToPom);
        }
    }

    private Integer saveConfigurationToPom() {
        telemetries.put(TELEMETRY_KEY_POM_FILE_MODIFIED, String.valueOf(true));
        this.clusterSettings.setSubscriptionId(this.subscriptionId);
        try {
            for (final MavenProject proj : targetProjects) {

                if (this.parentMode) {
                    this.appSettings.setAppName(this.appNameByProject.get(proj));
                    this.appSettings.setIsPublic((publicProjects != null && publicProjects.contains(proj)) ? "true" : "false");
                }
                saveConfigurationToProject(proj);
            }
            // add plugin to parent pom
            if (this.parentMode) {
                ConfigurationUpdater.updateAppConfigToPom(null, this.project, plugin);
            }
        } catch (DocumentException | IOException e) {
            throw Lombok.sneakyThrow(e);
        }
        return targetProjects.size();
    }

    private void saveConfigurationToProject(MavenProject proj) throws DocumentException, IOException {
        this.appSettings.setDeployment(this.deploymentSettings);
        this.appSettings.setCluster(this.clusterSettings);
        ConfigurationUpdater.updateAppConfigToPom(this.appSettings, proj, plugin);
    }

    private void configurePublic() throws IOException, InvalidConfigurationException {
        if (this.parentMode) {
            publicProjects = this.wrapper.handleMultipleCase("configure-public-list", targetProjects, MavenProject::getName);
        } else {
            this.appSettings.setIsPublic(this.wrapper.handle("configure-public", false));
        }

    }

    private void configureAppName() throws IOException, InvalidConfigurationException {
        if (StringUtils.isNotBlank(appName) && this.parentMode) {
            throw new UnsupportedOperationException("Cannot specify appName in parent mode.");
        }
        if (this.parentMode) {
            appNameByProject = new HashMap<>();
            for (final MavenProject proj : targetProjects) {
                this.wrapper.putCommonVariable("project", proj);
                this.appNameByProject.put(proj, this.wrapper.handle("configure-app-name", this.parentMode, this.appName));
            }
            // reset back of variable project
            this.wrapper.putCommonVariable("project", this.project);
            // handle duplicate app name
            final String duplicateAppNames = this.appNameByProject.values().stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream().filter(t -> t.getValue() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.joining(","));

            if (StringUtils.isNotBlank(duplicateAppNames)) {
                throw new InvalidConfigurationException(String.format("Cannot apply default appName due to duplicate: %s", duplicateAppNames));
            }
        } else {
            this.appSettings.setAppName(this.wrapper.handle("configure-app-name", false, this.appName));
        }

    }

    private SpringCloudApp selectApp(@Nonnull final SpringCloudCluster cluster) throws IOException, InvalidConfigurationException {
        log.info(String.format("It may take a few minutes to list apps in your Azure Spring Apps %s, please be patient.", cluster.getName()));
        if (StringUtils.isNotBlank(appName)) {
            final SpringCloudApp springCloudApp = cluster.apps().get(appName, resourceGroup);
            if (Objects.nonNull(springCloudApp) && springCloudApp.exists()) {
                appSettings.saveSpringCloudApp(springCloudApp);
            }
            log.warn(String.format("Cannot find app with name: %s in Azure Spring Apps: %s.",
                TextUtils.yellow(this.appName), TextUtils.yellow(cluster.getName())));
        }
        final List<SpringCloudApp> apps = cluster.apps().list();
        this.wrapper.putCommonVariable("apps", apps);
        final SpringCloudApp targetApp = this.wrapper.handleSelectOne("select-App", apps, null, AbstractAzResource::getName);
        appSettings.saveSpringCloudApp(targetApp);
        saveDeployment(targetApp);
        return targetApp;
    }

    private void saveDeployment(SpringCloudApp targetApp) throws IOException, InvalidConfigurationException {
        final SpringCloudDeployment activeDeployment = Optional.ofNullable(targetApp.getActiveDeployment())
            .orElseGet(() -> targetApp.deployments().list().stream().findFirst().orElse(null));
        if (Objects.nonNull(activeDeployment)) {
            deploymentSettings.saveSpringCloudDeployment(activeDeployment);
        } else {
            configCommon();
        }
    }

    private SpringCloudCluster selectAppCluster() throws IOException, InvalidConfigurationException {
        log.info("It may take a few minutes to list Azure Spring Apps in your account, please be patient.");
        final SpringCloudClusterModule az = Azure.az(AzureSpringCloud.class).clusters(subscriptionId);
        if (StringUtils.isNotBlank(clusterName)) {
            final SpringCloudCluster cluster = az.get(this.clusterName, this.resourceGroup);
            if (Objects.nonNull(cluster) && cluster.exists()) {
                this.clusterSettings.setResourceGroup(cluster.getResourceGroupName());
                this.clusterSettings.setClusterName(cluster.getName());
                return cluster;
            }
            log.warn(String.format("Cannot find Azure Spring Apps with name: %s in resource group: %s.",
                TextUtils.yellow(this.clusterName), TextUtils.yellow(this.resourceGroup)));
        }
        final List<SpringCloudCluster> clusters = az.list();
        this.wrapper.putCommonVariable("clusters", clusters);
        final SpringCloudCluster targetAppCluster = this.wrapper.handleSelectOne("select-ASC", clusters, null, AbstractAzResource::getName);
        if (targetAppCluster != null) {
            this.clusterSettings.setResourceGroup(targetAppCluster.getResourceGroupName());
            this.clusterSettings.setClusterName(targetAppCluster.getName());
            this.clusterSettings.setSku(targetAppCluster.getSku().toString());
            log.info(String.format("Using Azure Spring Apps: %s", TextUtils.blue(targetAppCluster.getName())));
        }
        return targetAppCluster;
    }

    @SneakyThrows
    protected void promptAndSelectSubscription() {
        if (StringUtils.isBlank(subscriptionId)) {
            final List<Subscription> subscriptions = Azure.az(AzureAccount.class).account().getSubscriptions();
            subscriptionId = (CollectionUtils.isNotEmpty(subscriptions) && subscriptions.size() == 1) ? subscriptions.get(0).getId() : promptSubscription();
        }
        // use selectSubscription to set selected subscriptions in account and print current subscription
        selectSubscription();
    }

    private String promptSubscription() throws IOException, InvalidConfigurationException {
        final List<Subscription> subscriptions = Azure.az(AzureAccount.class).account().getSubscriptions();
        final List<Subscription> selectedSubscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        this.wrapper.putCommonVariable("subscriptions", subscriptions);
        final Subscription select = this.wrapper.handleSelectOne("select-subscriptions", subscriptions,
            CollectionUtils.isNotEmpty(selectedSubscriptions) ? selectedSubscriptions.get(0) : null,
            t -> String.format("%s (%s)", t.getName(), t.getId()));
        com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().setSelectedSubscriptions(Collections.singletonList(select.getId()));
        return select.getId();
    }

    private boolean isProjectConfigured(MavenProject proj) {
        final String pluginIdentifier = plugin.getPluginLookupKey();
        final Xpp3Dom configuration = MavenConfigUtils.getPluginConfiguration(proj, pluginIdentifier);

        if (configuration == null) {
            return false;
        }

        for (final Xpp3Dom child : configuration.getChildren()) {
            if (APP_PROPERTIES.contains(child.getName())) {
                return true;
            }
        }

        if (configuration.getChild(DEPLOYMENT_TAG) != null) {
            for (final Xpp3Dom child : configuration.getChild(DEPLOYMENT_TAG).getChildren()) {
                if (DEPLOYMENT_PROPERTIES.contains(child.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
