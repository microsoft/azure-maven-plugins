/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParser;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParserFactory;
import com.microsoft.azure.maven.spring.spring.SpringServiceUtils;
import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import com.microsoft.azure.maven.telemetry.MojoStatus;
import com.microsoft.rest.LogLevel;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_AUTH_METHOD;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_CPU;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_INSTANCE_COUNT;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_KEY_ENCRYPTED;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_SERVICE_PRINCIPAL;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_JAVA_VERSION;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_JVM_PARAMETER;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_MEMORY;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_PUBLIC;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_WITHIN_PARENT_POM;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_VALUE_AUTH_POM_CONFIGURATION;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_CODE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_MESSAGE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_TYPE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_PLUGIN_NAME;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_PLUGIN_VERSION;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_ERROR_CODE_FAILURE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_ERROR_CODE_SUCCESS;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_SYSTEM_ERROR;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_USER_ERROR;

public abstract class AbstractSpringMojo extends AbstractMojo {

    @Parameter(property = "port")
    protected int port;

    @Parameter(alias = "public")
    protected boolean isPublic;

    @Parameter(property = "isTelemetryAllowed", defaultValue = "true")
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initExecution();
            doExecute();
            handleSuccess();
        } catch (Exception e) {
            handleException(e);
            throw e;
        } finally {
            AppInsightHelper.INSTANCE.close();
        }
    }

    protected void initExecution() {
        // Init sdk log level
        if (getLog().isDebugEnabled()) {
            SpringServiceUtils.setLogLevel(LogLevel.BODY_AND_HEADERS);
        }
        // Init telemetries
        telemetries = new HashMap<>();
        if (!isTelemetryAllowed) {
            AppInsightHelper.INSTANCE.disable();
        }
        tracePluginInformation();
        trackMojoExecution(MojoStatus.Start);
    }

    protected void handleSuccess() {
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_SUCCESS);
        trackMojoExecution(MojoStatus.Success);
    }

    protected void handleException(Exception exception) {
        final boolean isUserError = exception instanceof IllegalArgumentException || exception instanceof SpringConfigurationException;
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_FAILURE);
        telemetries.put(TELEMETRY_KEY_ERROR_TYPE, isUserError ? TELEMETRY_VALUE_USER_ERROR : TELEMETRY_VALUE_SYSTEM_ERROR);
        telemetries.put(TELEMETRY_KEY_ERROR_MESSAGE, exception.getMessage());
        trackMojoExecution(MojoStatus.Failure);
    }

    protected void trackMojoExecution(MojoStatus status) {
        final String eventName = String.format("%s.%s", this.getClass().getSimpleName(), status.name());
        AppInsightHelper.INSTANCE.trackEvent(eventName, getTelemetryProperties(), false);
    }

    protected void tracePluginInformation() {
        telemetries.put(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetries.put(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
        telemetries.put(TELEMETRY_KEY_WITHIN_PARENT_POM, String.valueOf(project.getPackaging().equalsIgnoreCase("pom")));
    }

    protected void traceConfiguration(SpringConfiguration configuration) {
        telemetries.put(TELEMETRY_KEY_PUBLIC, String.valueOf(configuration.isPublic()));
        telemetries.put(TELEMETRY_KEY_JAVA_VERSION, configuration.getJavaVersion());
        telemetries.put(TELEMETRY_KEY_CPU, String.valueOf(configuration.getDeployment().getCpu()));
        telemetries.put(TELEMETRY_KEY_MEMORY, String.valueOf(configuration.getDeployment().getMemoryInGB()));
        telemetries.put(TELEMETRY_KEY_INSTANCE_COUNT, String.valueOf(configuration.getDeployment().getInstanceCount()));
        telemetries.put(TELEMETRY_KEY_JVM_PARAMETER,
                String.valueOf(StringUtils.isEmpty(configuration.getDeployment().getJvmParameter())));
    }

    protected void traceAuth() {
        // Todo update deploy mojo telemetries with real value
        telemetries.put(TELEMETRY_KEY_AUTH_METHOD, TELEMETRY_VALUE_AUTH_POM_CONFIGURATION);
        telemetries.put(TELEMETRY_KEY_IS_SERVICE_PRINCIPAL, "false");
        telemetries.put(TELEMETRY_KEY_IS_KEY_ENCRYPTED, "false");
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

}
