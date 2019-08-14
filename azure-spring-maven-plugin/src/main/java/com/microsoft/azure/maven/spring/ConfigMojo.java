/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.microservices4spring.v2019_05_01_preview.implementation.AppClusterResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import com.microsoft.azure.maven.spring.prompt.IPrompter;
import com.microsoft.azure.maven.spring.spring.SpringServiceUtils;
import com.microsoft.azure.maven.spring.utils.MavenUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import com.microsoft.azure.maven.spring.utils.XmlUtils;
import com.microsoft.azure.maven.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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
import java.util.stream.Collectors;

@Mojo(name = "config", requiresProject = true, requiresDirectInvocation = true, aggregator = true)
public class ConfigMojo extends AbstractSpringMojo {
    private static final String DEPLOYMENT_TAG = "deployment";

    @Parameter(property = "full")
    private boolean full;

    private IPrompter prompt = null; // new DefaultPrompter();

    private List<MavenProject> targetProjects = new ArrayList<>();

    private List<MavenProject> publicProjects;

    private Authenticated azure;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        if (!settings.isInteractiveMode()) {
            throw new MojoFailureException("The goal 'config' must be run at interactive mode.");
        }

        if (!Utils.isPomPackagingProject(this.project) && !Utils.isJarPackagingProject(this.project)) {
            throw new MojoFailureException(String.format("The project (%s) with packaging %s is not supported for azure spring cloud service.",
                    this.project.getName(), this.project.getPackaging()));
        }
        if (isProjectConfigured(this.project)) {
            throw new MojoFailureException(String.format("Project (%s) is already configured.", this.project.getName()));
        }

        if (this.deployment == null) {
            // make sure deployment is not null
            this.deployment = new Deployment();
        }

        final boolean isRunningInParent = Utils.isPomPackagingProject(this.project);

        try {
            if (isRunningInParent) {
                if (full) {
                    throw new MojoFailureException("The \"full\" mode is not supported at parent folder.");
                }
                initializeProjects();
            }
            initializeCredentials();
            selectAppCluster();

            if (isRunningInParent) {
                configurePublicParent();
                confirmAndSaveParent();
            } else {
                configureAppName();
                configurePublic();
                if (full) {
                    configureInstanceCount();
                    configureCpu();
                    configureMemory();
                    configureJvmOptions();
                } else {
                    this.deployment.withCpu(1).withMemoryInGB(2).withInstanceCount(1).withJvmOptions("-Xmx1G");
                }
                confirmAndSave();
            }

            this.prompt.close();
        } catch (IOException | NoResourcesAvailableException | InvalidConfigurationException e) {
            throw new MojoFailureException(e.getMessage());
        }

        System.out.println(subscriptionId);
    }

    private void initializeProjects() throws MojoFailureException, IOException {
        final List<MavenProject> allProjects = session.getAllProjects().stream().filter(Utils::isJarPackagingProject).collect(Collectors.toList());
        final List<String> configuredProjects = new ArrayList<>();
        for (final MavenProject proj : allProjects) {
            if (isProjectConfigured(proj)) {
                configuredProjects.add(proj.getName());
            } else {
                targetProjects.add(proj);
            }
        }
        if (configuredProjects.size() > 0) {
            getLog().warn(String.format("Project (%s) %s already configured.",
                    TextUtils.yellow(configuredProjects.stream().collect(Collectors.joining(", "))), configuredProjects.size() > 1 ? "are" : "is"));
        }

        if (targetProjects.size() == 0) {
            getLog().warn("There are no projects in current folder which are not configured yet!");
            return;
        }
        selectProjects();
    }

    private void selectProjects() throws MojoFailureException, IOException {
        targetProjects = prompt.promoteMultipleEntities("project", targetProjects, MavenProject::getName, false, " for ALL projects", targetProjects);
    }

    private void configureJvmOptions() throws IOException {
        this.deployment.withJvmOptions(prompt.promoteString("jvmOptions", this.deployment.getJvmOptions(), ".*", false));
    }

    private void configureCpu() throws IOException {
        // TODO: default value
        this.deployment.withCpu(prompt.promoteInteger("cpu", 1, 1, 64, true));
    }

    private void configureMemory() throws IOException {
        // TODO: default value
        this.deployment.withMemoryInGB(prompt.promoteInteger("memory(GB)", 2, 1, 128, true));
    }

    private void configureInstanceCount() throws IOException {
        // TODO: default value
        this.deployment.withInstanceCount(prompt.promoteInteger("instanceCount", 1, 1, 10, true));
    }

    private void confirmAndSave() throws IOException {
        System.out.println("Please confirm the following properties");
        System.out.println("subscription id: " + TextUtils.green(this.subscriptionId));
        System.out.println("service name   : " + TextUtils.green(this.clusterName));
        System.out.println("app name       : " + TextUtils.green(this.appName));
        if (this.isPublic != null && this.isPublic.booleanValue()) {
            System.out.println("public access  : " + TextUtils.green(this.isPublic.toString()));
        }
        if (this.deployment.getInstanceCount() != null) {
            System.out.println("instance count : " + TextUtils.green(this.deployment.getInstanceCount().toString()));
        }

        if (this.deployment.getCpu() != null) {
            System.out.println("cpu            : " + TextUtils.green(this.deployment.getCpu().toString()));
        }

        if (this.deployment.getMemoryInGB() != null) {
            System.out.println("memory(GB)     : " + TextUtils.green(this.deployment.getMemoryInGB().toString()));
        }

        if (StringUtils.isNotEmpty(this.deployment.getJvmOptions())) {
            System.out.println("jvm options    : " + TextUtils.green(this.deployment.getJvmOptions()));
        }

        if (prompt.promoteYesNo(true, "Confirm to save all the properties listed above?", true)) {
            saveConfigurationToProject(this.project, true);
            getLog().info("These changes are saved to the pom.xml file.");
        }
    }

    private void confirmAndSaveParent() throws IOException {
        System.out.println("Please confirm the following properties");
        System.out.println("subscription id : " + TextUtils.green(this.subscriptionId));
        System.out.println("service name    : " + TextUtils.green(this.clusterName));
        if (this.publicProjects != null && this.publicProjects.size() > 0) {
            System.out
                    .println("public app(s)  : " + TextUtils.green(publicProjects.stream().map(t -> t.getName()).collect(Collectors.joining(", "))));
        }
        if (prompt.promoteYesNo(true, "Confirm to save all the properties listed above?", true)) {
            saveConfigurationParent();
            getLog().info("These changes are saved to the related pom.xml files.");
        }
    }

    private void saveConfigurationParent() {
        for (final MavenProject proj : targetProjects) {
            this.isPublic = this.publicProjects.contains(proj);
            saveConfigurationToProject(proj, false);
        }
    }

    private void saveConfigurationToProject(MavenProject proj, boolean includeDeployment) {
        final Model model = proj.getOriginalModel();
        final String pluginIdentifer = this.plugin.getPluginLookupKey();
        Plugin target = MavenUtils.getPluginFromMavenModel(proj.getOriginalModel(), pluginIdentifer, false);
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
        XmlUtils.replaceDomWithKeyValue(config, "clusterName", this.clusterName);
        XmlUtils.replaceDomWithKeyValue(config, "appName", this.appName);
        if (this.isPublic != null) {
            XmlUtils.replaceDomWithKeyValue(config, "isPublic", this.isPublic);
        }
        if (includeDeployment && this.deployment != null) {
            if (config.getChild(DEPLOYMENT_TAG) == null) {
                config.addChild(XmlUtils.createDomWithName(DEPLOYMENT_TAG));
            }
            this.deployment.applyToXpp3Dom(config.getChild(DEPLOYMENT_TAG));
        }

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

            final Format format = Format.getPrettyFormat().setEncoding(encoding).setTextMode(TextMode.PRESERVE);
            writer = WriterFactory.newWriter(pom, encoding);
            new MavenJDOMWriter().write(model, doc, writer, format);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IOUtil.close(writer);
        }
    }

    private void configurePublic() throws IOException {
        this.isPublic = prompt.promoteYesNo(false, "Do you want to enable public access for this app?", true);
    }

    private void configurePublicParent() throws IOException {
        publicProjects = prompt.promoteMultipleEntities("project", targetProjects, t -> t.getName(), true, " to select none",
                new ArrayList<MavenProject>());
    }

    private void configureAppName() throws IOException {
        appName = prompt.promoteString("app name", "${project.artifactId}", "[A-Za-z0-9_\\.]+", true);
    }

    private void selectAppCluster() throws IOException, NoResourcesAvailableException {
        final List<AppClusterResourceInner> clusters = SpringServiceUtils.getAvailableClusters(this.subscriptionId);
        final AppClusterResourceInner clusterByName = clusters.stream().filter(t -> StringUtils.equals(this.clusterName, t.name())).findFirst()
                .orElse(null);
        if (clusterByName == null) {
            clusterName = prompt.promoteSingleEntity("service", clusters, Utils.firstOrNull(clusters), t -> t.name(), true).name();
        }

        getLog().info(String.format("Using service: %s", TextUtils.blue(clusterName)));
    }

    private void initializeCredentials() throws InvalidConfigurationException, IOException, NoResourcesAvailableException {
        // TODO: getAzureTokenCredentials will check auth for null, but maven will always map a default AuthConfiguration
        final AzureTokenCredentials cred = AzureAuthHelper.getAzureTokenCredentials(this.auth);
        azure = Azure.configure().authenticate(cred);
        if (StringUtils.isBlank(subscriptionId)) {
            subscriptionId = StringUtils.isBlank(cred.defaultSubscriptionId()) ? selectSubscription() : cred.defaultSubscriptionId();
        }
    }

    private String selectSubscription() throws IOException, NoResourcesAvailableException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        final Subscription select = this.prompt.promoteSingleEntity("subscription", subscriptions, Utils.firstOrNull(subscriptions),
            t -> String.format("%s (%s)", t.displayName(), t.subscriptionId()), true);

        return select.subscriptionId();
    }

    private boolean isProjectConfigured(MavenProject proj) {
        final String pluginIdentifer = plugin.getPluginLookupKey();
        final Xpp3Dom configuration = MavenUtils.getPluginConfiguration(proj, pluginIdentifer);
        final List<String> topLevelProperties = Arrays.asList("subscriptionId", "appName", "isPublic", "runtimeVersion");

        if (configuration == null) {
            return false;
        }

        for (final Xpp3Dom child : configuration.getChildren()) {
            if (topLevelProperties.contains(child.getName())) {
                return true;
            }
        }

        final List<String> deploymentProperties = Arrays.asList("vCPU", "memoryInGB", "instanceCount", "jvmOptions");
        if (configuration.getChild(DEPLOYMENT_TAG) != null) {
            for (final Xpp3Dom child : configuration.getChild(DEPLOYMENT_TAG).getChildren()) {
                if (deploymentProperties.contains(child.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
