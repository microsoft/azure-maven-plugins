/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.applicationinsights.internal.channel.common.ApacheSenderFactory;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParser;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParserFactory;
import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSpringMojo extends AbstractMojo {

    private static final String TELEMETRY_KEY_PLUGIN_NAME = "pluginName";
    private static final String TELEMETRY_KEY_PLUGIN_VERSION = "pluginVersion";

    @Parameter(property = "port")
    protected int port;

    @Parameter(alias = "public")
    protected boolean isPublic;

    @Parameter(property = "isTelemetryAllowed")
    protected boolean isTelemetryAllowed;

    @Parameter(property = "subscriptionId")
    protected String subscriptionId;

    @Parameter(property = "resourceGroup")
    protected String resourceGroup;

    @Parameter(property = "clusterName")
    protected String clusterName;

    @Parameter(property = "appName")
    protected String appName;

    @Parameter(property = "javaVersion")
    protected String javaVersion;

    @Parameter(property = "deployment")
    protected Deployment deployment;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    protected File buildDirectory;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor plugin;

    protected Map<String, String> telemetries;

    public AbstractSpringMojo() {
        telemetries = new HashMap<>();
        telemetries.put(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetries.put(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initExecution();
            doExecute();
            handleSuccess();
        } catch (Exception e) {
            handleException(e);
        } finally {
            // When maven goal executes too quick, The HTTPClient of AI SDK may not fully initialized and will step
            // into endless loop when close, we need to call it in main thread.
            // Refer here for detail codes: https://github.com/Microsoft/ApplicationInsights-Java/blob/master/core/src
            // /main/java/com/microsoft/applicationinsights/internal/channel/common/ApacheSender43.java#L103
            try {
                // Sleep to wait ai sdk flush telemetries
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                // swallow this exception
            }
            ApacheSenderFactory.INSTANCE.create().close();
        }
    }

    protected void initExecution() {
        if (isTelemetryAllowed) {
            AppInsightHelper.INSTANCE.disable();
        }
        trackMojoExecution(MojoStatus.Start);
    }

    protected void handleSuccess() {
        telemetries.put("errorCode", "0");
        trackMojoExecution(MojoStatus.Success);
    }

    protected void handleException(Exception exception) {
        final boolean isUserError = exception instanceof IllegalArgumentException || exception instanceof SpringConfigurationException;
        telemetries.put("errorCode", "1");
        telemetries.put("errorType", isUserError ? "userError" : "systemError");
        telemetries.put("errorMessage", exception.getMessage());
        trackMojoExecution(MojoStatus.Failure);
    }

    protected void trackMojoExecution(MojoStatus status) {
        final String eventName = String.join("%s.%s", this.getClass().getSimpleName(), status.name());
        AppInsightHelper.INSTANCE.trackEvent(eventName, getTelemetryProperties(), false);
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    public int getPort() {
        return port;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public MavenProject getProject() {
        return project;
    }

    public MavenSession getSession() {
        return session;
    }

    public File getBuildDirectory() {
        return buildDirectory;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public SpringConfiguration getConfiguration() {
        final SpringConfigurationParser parser = SpringConfigurationParserFactory.INSTANCE.getConfigurationParser();
        return parser.parse(this);
    }

    public Map<String, String> getTelemetryProperties() {
        return telemetries;
    }

    enum MojoStatus {
        Start,
        Skip,
        Success,
        Failure
    }
}
