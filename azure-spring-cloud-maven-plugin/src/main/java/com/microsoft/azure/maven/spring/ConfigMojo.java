/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.common.utils.SneakyThrowUtils;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.maven.common.utils.MavenUtils;
import com.microsoft.azure.maven.spring.configuration.AppSettings;
import com.microsoft.azure.maven.spring.configuration.DeploymentSettings;
import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.pom.PomXmlUpdater;
import com.microsoft.azure.maven.spring.prompt.PromptWrapper;
import com.microsoft.azure.maven.spring.utils.Utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_POM_FILE_MODIFIED;

/**
 * The Mojo for 'config' goal.
 */
@Mojo(name = "config", requiresDirectInvocation = true, aggregator = true)
public class ConfigMojo extends AbstractSpringMojo {
    private static final String DEPLOYMENT_TAG = "deployment";
    private static final List<String> APP_PROPERTIES = Arrays.asList("subscriptionId", "appName", "isPublic");
    private static final List<String> DEPLOYMENT_PROPERTIES = Arrays.asList("cpu", "memoryInGB", "instanceCount", "jvmOptions", "runtimeVersion");

    private boolean parentMode;

    /**
     * The prompt wrapper to get user input for each properties.
     */
    private PromptWrapper wrapper;

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
     * The azure client for get list of subscriptions.
     */
    private Authenticated azure;

    /**
     * The app settings collected from user.
     */
    private AppSettings appSettings;

    /**
     * The deployment settings collected from user.
     */
    private DeploymentSettings deploymentSettings;

    /**
     * The maven variable used to evaluate maven expression.
     */
    @Parameter(defaultValue = "${mojoExecution}")
    protected MojoExecution mojoExecution;

    /**
     * The parameter which controls whether or not the advanced options should be prompted to user
     */
    @Parameter(property = "advancedOptions")
    private boolean advancedOptions;

    @Override
    protected void doExecute() throws MojoFailureException {
        if (!settings.isInteractiveMode()) {
            throw new UnsupportedOperationException("The goal 'config' must be run at interactive mode.");
        }

        if (!Utils.isPomPackagingProject(this.project) && !Utils.isJarPackagingProject(this.project)) {
            throw new UnsupportedOperationException(
                    String.format("The project (%s) with packaging %s is not supported for azure spring cloud service.", this.project.getName(),
                            this.project.getPackaging()));
        }
        if (isProjectConfigured(this.project)) {
            getLog().warn(String.format("Project (%s) is already configured and won't be affected by this command.", this.project.getName()));
            return;
        }
        appSettings = new AppSettings();
        deploymentSettings = new DeploymentSettings();
        parentMode = Utils.isPomPackagingProject(this.project);
        if (parentMode && advancedOptions) {
            throw new UnsupportedOperationException("The \"advancedOptions\" mode is not supported at parent folder.");
        }
        final ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        try {
            this.wrapper = new PromptWrapper(expressionEvaluator, getLog());
            this.wrapper.initialize();
            this.wrapper.putCommonVariable("project", this.project);

            selectProjects();
            if (targetProjects == null || targetProjects.isEmpty()) {
                // no need to proceed when there are no projects need to be configured
                return;
            }
            selectSubscription();

            selectAppCluster();
            configCommon();
            confirmAndSave();
        } catch (IOException | InvalidConfigurationException | SpringConfigurationException |
                UnsupportedOperationException e) {
            throw new MojoFailureException(e.getMessage());
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

    private void configCommon() throws IOException, NoResourcesAvailableException, InvalidConfigurationException {
        configureAppName();
        configurePublic();
        configureInstanceCount();
        configureCpu();
        configureMemory();
        configureJavaVersion();
        configureJvmOptions();
    }

    private void selectProjects() throws IOException, NoResourcesAvailableException {
        if (this.parentMode) {
            final List<MavenProject> allProjects = session.getAllProjects().stream().filter(Utils::isJarPackagingProject)
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
            changesToConfirm.put("Public access", this.appSettings.isPublic());
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
                    this.appSettings.setPublic((publicProjects != null && publicProjects.contains(proj)) ? "true" : "false");
                }
                saveConfigurationToProject(proj);
            }
            // add plugin to parent pom
            if (this.parentMode) {
                new PomXmlUpdater(this.project, plugin).updateSettings(null, null);
            }
        } catch (DocumentException | IOException e) {
            return SneakyThrowUtils.sneakyThrow(e);
        }
        return targetProjects.size();
    }

    private void saveConfigurationToProject(MavenProject proj) throws DocumentException, IOException {
        new PomXmlUpdater(proj, plugin).updateSettings(this.appSettings, this.deploymentSettings);
    }

    private void configurePublic() throws IOException, NoResourcesAvailableException, InvalidConfigurationException {
        if (this.parentMode) {
            publicProjects = this.wrapper.handleMultipleCase("configure-public-list", targetProjects, MavenProject::getName);
        } else {
            this.appSettings.setPublic(this.wrapper.handle("configure-public", false));
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

    private void selectAppCluster() throws IOException, SpringConfigurationException {
        final List<ServiceResourceInner> clusters = getSpringServiceClient().getAvailableClusters();

        this.wrapper.putCommonVariable("clusters", clusters);
        if (StringUtils.isNotBlank(clusterName)) {
            final ServiceResourceInner clusterByName = clusters.stream().filter(t -> StringUtils.equals(this.clusterName, t.name())).findFirst()
                    .orElse(null);
            if (clusterByName != null) {

                this.appSettings.setClusterName(clusterByName.name());
                return;
            }
            getLog().warn(String.format("Cannot find Azure Spring Cloud Service with name: %s.", TextUtils.yellow(this.clusterName)));
        }

        final ServiceResourceInner targetAppCluster = this.wrapper.handleSelectOne("select-ASC", clusters, null, ServiceResourceInner::name);
        if (targetAppCluster != null) {
            this.appSettings.setClusterName(targetAppCluster.name());
            getLog().info(String.format("Using service: %s", TextUtils.blue(targetAppCluster.name())));
        }
    }

    private void selectSubscription() throws IOException, SpringConfigurationException {
        // TODO: getAzureTokenCredentials will check auth for null, but maven will always map a default AuthConfiguration
        azure = Azure.configure().authenticate(azureTokenCredentials);
        if (StringUtils.isBlank(subscriptionId)) {
            subscriptionId = StringUtils.isBlank(azureTokenCredentials.defaultSubscriptionId()) ? promptSubscription() :
                    azureTokenCredentials.defaultSubscriptionId();
        }
    }

    private String promptSubscription() throws IOException, SpringConfigurationException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        this.wrapper.putCommonVariable("subscriptions", subscriptions);
        final Subscription select = this.wrapper.handleSelectOne("select-subscriptions", subscriptions, null,
            t -> String.format("%s (%s)", t.displayName(), t.subscriptionId()));
        return select.subscriptionId();
    }

    private boolean isProjectConfigured(MavenProject proj) {
        final String pluginIdentifier = plugin.getPluginLookupKey();
        final Xpp3Dom configuration = MavenUtils.getPluginConfiguration(proj, pluginIdentifier);

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
