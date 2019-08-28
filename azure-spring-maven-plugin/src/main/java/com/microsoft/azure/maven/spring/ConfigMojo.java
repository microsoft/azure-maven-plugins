/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.maven.spring.configuration.AppSettings;
import com.microsoft.azure.maven.spring.configuration.DeploymentSettings;
import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.prompt.PromptWrapper;
import com.microsoft.azure.maven.spring.utils.MavenUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import com.microsoft.azure.maven.spring.utils.XmlUtils;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.atteo.evo.inflector.English;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.Format.TextMode;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Mojo for 'config' goal.
 */
@Mojo(name = "config", requiresProject = true, requiresDirectInvocation = true, aggregator = true)
public class ConfigMojo extends AbstractSpringMojo {
    private static final String DEPLOYMENT_TAG = "deployment";
    private static final List<String> APP_PROPERTIES = Arrays.asList("subscriptionId", "appName", "isPublic", "runtimeVersion");
    private static final List<String> DEPLOYMENT_PROPERTIES = Arrays.asList("cpu", "memoryInGB", "instanceCount", "jvmOptions");

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
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!settings.isInteractiveMode()) {
            throw new UnsupportedOperationException("The goal 'config' must be run at interactive mode.");
        }

        if (!Utils.isPomPackagingProject(this.project) && !Utils.isJarPackagingProject(this.project)) {
            throw new UnsupportedOperationException(String.format("The project (%s) with packaging %s is not supported for azure spring cloud service.",
                    this.project.getName(), this.project.getPackaging()));
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
        } catch (IOException | InvalidConfigurationException | SpringConfigurationException | ExpressionEvaluationException | UnsupportedOperationException e) {
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

    private void configCommon() throws IOException, ExpressionEvaluationException, NoResourcesAvailableException, InvalidConfigurationException {
        configureAppName();
        configurePublic();
        configureInstanceCount();
        configureCpu();
        configureMemory();
        configureJvmOptions();
    }

    private void selectProjects() throws MojoFailureException, IOException, NoResourcesAvailableException {
        if (this.parentMode) {
            final List<MavenProject> allProjects = session.getAllProjects().stream().filter(Utils::isJarPackagingProject).collect(Collectors.toList());
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

    private void configureJvmOptions() throws IOException, ExpressionEvaluationException, InvalidConfigurationException {
        this.deploymentSettings.withJvmOptions(this.wrapper.handle("configure-jvm-options", !advancedOptions || parentMode));
    }

    private void configureCpu() throws IOException, ExpressionEvaluationException, InvalidConfigurationException {
        this.deploymentSettings.withCpu(this.wrapper.handle("configure-cpu", !advancedOptions || parentMode));
    }

    private void configureMemory() throws IOException, ExpressionEvaluationException, InvalidConfigurationException {
        this.deploymentSettings.withMemoryInGB(this.wrapper.handle("configure-memory", !advancedOptions || parentMode));
    }

    private void configureInstanceCount() throws IOException, ExpressionEvaluationException, InvalidConfigurationException {
        this.deploymentSettings.withInstanceCount(this.wrapper.handle("configure-instance-count", !advancedOptions || parentMode));
    }

    private void confirmAndSave() throws IOException {
        this.wrapper.confirmCommonHeader();
        confirmCommon();
        if (this.parentMode) {
            if (this.publicProjects != null && this.publicProjects.size() > 0) {
                printConfirmation("Public " + English.plural("app", this.publicProjects.size()),
                        publicProjects.stream().map(t -> t.getName()).collect(Collectors.joining(", ")));
            }
            if (this.wrapper.confirmCommonFooter(getLog())) {
                saveConfigurationParent();
                this.wrapper.printConfirmResult(this.targetProjects.size(), getLog());
            }
        } else {
            printConfirmation("App name", this.appSettings.getAppName());

            if (StringUtils.isNotBlank(this.appSettings.isPublic())) {
                printConfirmation("Public access", this.appSettings.isPublic());
            }

            if (StringUtils.isNotBlank(this.deploymentSettings.getInstanceCount())) {
                printConfirmation("Instance count", this.deploymentSettings.getInstanceCount());
            }

            if (StringUtils.isNotBlank(this.deploymentSettings.getCpu())) {
                printConfirmation("CPU count", this.deploymentSettings.getCpu());
            }

            if (StringUtils.isNotBlank(this.deploymentSettings.getMemoryInGB())) {
                printConfirmation("Memory size(GB)", this.deploymentSettings.getMemoryInGB());
            }

            if (StringUtils.isNotBlank(this.deploymentSettings.getJvmOptions())) {
                printConfirmation("JVM options", this.deploymentSettings.getJvmOptions());
            }

            if (this.wrapper.confirmCommonFooter(getLog())) {
                saveConfigurationToProject(this.project);
                this.wrapper.printConfirmResult(this.targetProjects.size(), getLog());
            }
        }
    }

    private void confirmCommon() {
        printConfirmation("Subscription id", this.subscriptionId);
        printConfirmation("Service name", this.appSettings.getClusterName());
    }

    private void saveConfigurationParent() {
        for (final MavenProject proj : targetProjects) {
            this.appSettings.setIsPublic((publicProjects != null && publicProjects.contains(proj)) ? "true" : "false");
            saveConfigurationToProject(proj);
        }
    }

    private void saveConfigurationToProject(MavenProject proj) {
        final Model model = proj.getOriginalModel();
        final String pluginIdentifier = this.plugin.getPluginLookupKey();
        Plugin target = MavenUtils.getPluginFromMavenModel(proj.getOriginalModel(), pluginIdentifier, false);
        if (target == null) {
            target = MavenUtils.createPlugin(this.plugin.getGroupId(), this.plugin.getArtifactId(), this.plugin.getVersion());
            if (model.getBuild() == null) {
                model.setBuild(new Build());
            }
            model.getBuild().addPlugin(target);
        }
        if (StringUtils.isBlank(target.getVersion())) {
            target.setVersion(this.plugin.getVersion());
        }

        if (target.getConfiguration() == null) {
            target.setConfiguration(new Xpp3Dom("configuration"));
        }
        final Xpp3Dom config = (Xpp3Dom) target.getConfiguration();

        XmlUtils.replaceDomWithKeyValue(config, "subscriptionId", this.subscriptionId);
        this.appSettings.applyToXpp3Dom(config);
        if (config.getChild(DEPLOYMENT_TAG) == null) {
            config.addChild(XmlUtils.createDomWithName(DEPLOYMENT_TAG));
        }

        this.deploymentSettings.applyToXpp3Dom(config.getChild(DEPLOYMENT_TAG));

        Writer writer = null;
        try {
            // this code is copied from https://github.com/Commonjava/maven-model-jdom-support
            // which can apply the xml changes and preserve existing order of elements, formatting, and comments
            final File pom = proj.getFile();
            final SAXBuilder builder = new SAXBuilder();
            builder.setIgnoringBoundaryWhitespace(false);
            builder.setIgnoringElementContentWhitespace(false);
            final Document doc = builder.build(pom);
            String encoding = model.getModelEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }

            final Format format = Format.getRawFormat().setEncoding(encoding).setTextMode(TextMode.PRESERVE);
            writer = WriterFactory.newWriter(pom, encoding);
            new MavenJDOMWriter().write(model, doc, writer, format);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IOUtil.close(writer);
        }
    }

    private void configurePublic() throws IOException, NoResourcesAvailableException, ExpressionEvaluationException, InvalidConfigurationException {
        if (this.parentMode) {
            publicProjects = this.wrapper.handleMultipleCase("configure-public-list", targetProjects, MavenProject::getName);
        } else {
            this.appSettings.setIsPublic(this.wrapper.handle("configure-public", false));
        }

    }

    private void configureAppName() throws IOException, ExpressionEvaluationException, InvalidConfigurationException {
        if (StringUtils.isNotBlank(appName)) {
            if (this.parentMode) {
                throw new UnsupportedOperationException("Cannot specify appName in parent mode.");
            }
        }
        this.appSettings.setAppName(this.wrapper.handle("configure-app-name", this.parentMode, this.appName));
    }

    private void selectAppCluster() throws IOException, NoResourcesAvailableException, MojoFailureException, SpringConfigurationException {
        final List<AppClusterResourceInner> clusters = getSpringServiceClient().getAvailableClusters();

        this.wrapper.putCommonVariable("clusters", clusters);
        if (StringUtils.isNotBlank(clusterName)) {
            final AppClusterResourceInner clusterByName = clusters.stream().filter(t -> StringUtils.equals(this.clusterName, t.name())).findFirst()
                    .orElse(null);
            if (clusterByName != null) {

                this.appSettings.setClusterName(clusterByName.name());
                return;
            }
            getLog().warn(String.format("Cannot find Azure Spring Cloud Service with name: %s.", TextUtils.yellow(this.clusterName)));
        }

        final AppClusterResourceInner targetAppCluster = this.wrapper.handleSelectOne("select-ASC", clusters, null, AppClusterResourceInner::name
                );
        if (targetAppCluster != null) {
            this.appSettings.setClusterName(targetAppCluster.name());
            getLog().info(String.format("Using service: %s", TextUtils.blue(targetAppCluster.name())));
        }
    }

    private void selectSubscription() throws InvalidConfigurationException, IOException, SpringConfigurationException {
        // TODO: getAzureTokenCredentials will check auth for null, but maven will always map a default AuthConfiguration
        azure = Azure.configure().authenticate(azureTokenCredentials);
        if (StringUtils.isBlank(subscriptionId)) {
            subscriptionId = StringUtils.isBlank(azureTokenCredentials.defaultSubscriptionId()) ? promptSubscription() :
                    azureTokenCredentials.defaultSubscriptionId();
        }
    }

    private String promptSubscription() throws IOException, NoResourcesAvailableException, SpringConfigurationException {
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

    private static void printConfirmation(String key, Object value) {
        System.out.printf("%-17s : %s%n", key, TextUtils.green(Objects.toString(value)));
    }
}
