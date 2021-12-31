/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.springcloud.config.AppDeploymentRawConfig;
import com.microsoft.azure.maven.springcloud.config.AppRawConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationPrompter;
import com.microsoft.azure.maven.springcloud.config.ConfigurationUpdater;
import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudClusterModule;
import lombok.Lombok;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate configuration for spring cloud maven plugin or init the configuration from existing Azure Spring Cloud app instance.
 */
@Mojo(name = "config", requiresDirectInvocation = true, aggregator = true)
public class ConfigMojo extends AbstractMojoBase {
    private static final String DEPLOYMENT_TAG = "deployment";
    private static final List<String> APP_PROPERTIES = Arrays.asList("appName", "isPublic");
    private static final List<String> DEPLOYMENT_PROPERTIES = Arrays.asList("cpu", "memoryInGB", "instanceCount", "jvmOptions", "runtimeVersion");

    private boolean parentMode;

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
    private boolean advancedOptions;

    @Override
    protected void doExecute() throws AzureExecutionException {
        if (!settings.isInteractiveMode()) {
            throw new UnsupportedOperationException("The goal 'config' must be run at interactive mode.");
        }

        if (!MavenConfigUtils.isPomPackaging(this.project) && !MavenConfigUtils.isJarPackaging(this.project)) {
            throw new UnsupportedOperationException(
                    String.format("The project (%s) with packaging %s is not supported for azure spring cloud service.", this.project.getName(),
                            this.project.getPackaging()));
        }
        if (isProjectConfigured(this.project)) {
            getLog().warn(String.format("Project (%s) is already configured and won't be affected by this command.", this.project.getName()));
            return;
        }
        appSettings = new AppRawConfig();
        deploymentSettings = new AppDeploymentRawConfig();
        parentMode = MavenConfigUtils.isPomPackaging(this.project);
        if (parentMode && advancedOptions) {
            throw new UnsupportedOperationException("The \"advancedOptions\" mode is not supported at parent folder.");
        }
        final ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        try {
            this.wrapper = new ConfigurationPrompter(expressionEvaluator, getLog());
            this.wrapper.initialize();
            this.wrapper.putCommonVariable("project", this.project);

            selectProjects();
            if (targetProjects == null || targetProjects.isEmpty()) {
                // no need to proceed when there are no projects need to be configured
                return;
            }
            // select subscription in spring cloud -> config is different from other goals since it is prompted after select project.
            // set up account and select subscription here
            getAzureAccount();
            promptAndSelectSubscription();

            selectAppCluster();
            configCommon();
            confirmAndSave();
        } catch (IOException | InvalidConfigurationException | UnsupportedOperationException | MavenDecryptException | LoginFailureException e) {
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

    private void configCommon() throws IOException, InvalidConfigurationException {
        configureAppName();
        configurePublic();
        configureInstanceCount();
        configureCpu();
        configureMemory();
        configureJavaVersion();
        configureJvmOptions();
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
                getLog().warn(String.format("The following child %s %s already configured: ", English.plural("module", configuredProjects.size()),
                        configuredProjects.size() > 1 ? "are" : "is"));
                for (final MavenProject proj : configuredProjects) {
                    System.out.println("    " + TextUtils.yellow(proj.getName()));
                }
            } else if (targetProjects.isEmpty()) {
                getLog().warn("There are no projects in current folder with package 'jar'.");
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
        this.deploymentSettings.setRuntimeVersion(this.wrapper.handle("configure-java-version", autoUseDefault()));
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
        return !advancedOptions || parentMode;
    }

    private void confirmAndSave() throws IOException {
        final Map<String, String> changesToConfirm = new LinkedHashMap<>();
        changesToConfirm.put("Subscription id", this.subscriptionId);
        changesToConfirm.put("Service name", this.appSettings.getClusterName());

        if (this.parentMode) {
            if (this.publicProjects != null && this.publicProjects.size() > 0) {
                changesToConfirm.put("Public " + English.plural("app", this.publicProjects.size()),
                        publicProjects.stream().map(MavenProject::getName).collect(Collectors.joining(",")));
            }

            changesToConfirm.put("App " + English.plural("name", this.appNameByProject.size()),
                    String.join(",", appNameByProject.values()));

            this.wrapper.confirmChanges(changesToConfirm, this::saveConfigurationToPom);
        } else {
            changesToConfirm.put("App name", this.appSettings.getAppName());
            changesToConfirm.put("Public access", this.appSettings.getIsPublic());
            changesToConfirm.put("Instance count", this.deploymentSettings.getInstanceCount());
            changesToConfirm.put("CPU count", this.deploymentSettings.getCpu());
            changesToConfirm.put("Memory size(GB)", this.deploymentSettings.getMemoryInGB());
            changesToConfirm.put("JVM options", this.deploymentSettings.getJvmOptions());
            changesToConfirm.put("Runtime Java version", this.deploymentSettings.getRuntimeVersion());
            this.wrapper.confirmChanges(changesToConfirm, this::saveConfigurationToPom);
        }
    }

    private Integer saveConfigurationToPom() {
        telemetries.put(TELEMETRY_KEY_POM_FILE_MODIFIED, String.valueOf(true));
        this.appSettings.setSubscriptionId(this.subscriptionId);
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

    private void selectAppCluster() throws IOException, InvalidConfigurationException {
        final SpringCloudClusterModule az = Azure.az(AzureSpringCloud.class).clusters(subscriptionId);
        if (StringUtils.isNotBlank(clusterName)) {
            final SpringCloudCluster cluster = az.get(this.clusterName, null);
            if (Objects.nonNull(cluster) && cluster.exists()) {
                this.appSettings.setClusterName(cluster.getName());
                return;
            }
            getLog().warn(String.format("Cannot find Azure Spring Cloud Service with name: %s.", TextUtils.yellow(this.clusterName)));
        }
        final List<SpringCloudCluster> clusters = az.list();
        this.wrapper.putCommonVariable("clusters", clusters);
        final SpringCloudCluster targetAppCluster = this.wrapper.handleSelectOne("select-ASC", clusters, null, AbstractAzResource::getName);
        if (targetAppCluster != null) {
            this.appSettings.setClusterName(targetAppCluster.name());
            getLog().info(String.format("Using service: %s", TextUtils.blue(targetAppCluster.name())));
        }
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
        com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().selectSubscription(Collections.singletonList(select.getId()));
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
