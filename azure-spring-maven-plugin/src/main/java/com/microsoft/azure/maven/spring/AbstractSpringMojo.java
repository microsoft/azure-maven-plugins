/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.MavenSettingHelper;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.auth.exception.MavenDecryptException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.maven.spring.configuration.Deployment;
import com.microsoft.azure.maven.spring.configuration.SpringConfiguration;
import com.microsoft.azure.maven.spring.exception.SpringConfigurationException;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParser;
import com.microsoft.azure.maven.spring.parser.SpringConfigurationParserFactory;
import com.microsoft.azure.maven.spring.spring.SpringServiceClient;
import com.microsoft.azure.maven.spring.utils.MavenUtils;
import com.microsoft.azure.maven.spring.utils.Utils;
import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import com.microsoft.azure.maven.telemetry.MojoStatus;
import com.microsoft.azure.maven.validation.ConfigurationProblem;
import com.microsoft.azure.maven.validation.ConfigurationProblem.Severity;
import com.microsoft.rest.LogLevel;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_AUTH_METHOD;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_CPU;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_INSTANCE_COUNT;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_KEY_ENCRYPTED;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_IS_SERVICE_PRINCIPAL;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_JVM_OPTIONS;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_MEMORY;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_PUBLIC;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_RUNTIME_VERSION;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_KEY_WITHIN_PARENT_POM;
import static com.microsoft.azure.maven.spring.TelemetryConstants.TELEMETRY_VALUE_AUTH_POM_CONFIGURATION;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_DURATION;
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
    @Parameter(property = "auth")
    protected AuthConfiguration auth;

    @Parameter(alias = "public")
    protected Boolean isPublic;

    @Parameter(property = "isTelemetryAllowed", defaultValue = "true")
    protected boolean isTelemetryAllowed;

    @Parameter(property = "subscriptionId")
    protected String subscriptionId;

    @Parameter(property = "clusterName")
    protected String clusterName;

    @Parameter(property = "appName")
    protected String appName;

    @Parameter(property = "runtimeVersion")
    protected String runtimeVersion;

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

    @Component
    protected SettingsDecrypter settingsDecrypter;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    // todo: Remove this parameter before release
    @Parameter(property = "dogFood", defaultValue = "false")
    protected Boolean dogFood;

    protected AzureTokenCredentials azureTokenCredentials;

    protected SpringServiceClient springServiceClient;

    protected Long timeStart;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initExecution();
            doExecute();
            handleSuccess();
        } catch (Exception e) {
            handleException(e);
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            AppInsightHelper.INSTANCE.close();
        }
    }

    protected void initExecution() throws MojoFailureException, InvalidConfigurationException, IOException, DesktopNotSupportedException,
            ExecutionException, AzureLoginFailureException, InterruptedException {
        // Init telemetries
        timeStart = System.currentTimeMillis();
        telemetries = new HashMap<>();
        if (!isTelemetryAllowed) {
            AppInsightHelper.INSTANCE.disable();
        }
        initializeAuthConfiguration();

        final AuthConfiguration authConfiguration = isAuthConfigurationExist() ? auth : null;
        this.azureTokenCredentials = dogFood ? Utils.getCredential() : AzureAuthHelper.getAzureTokenCredentials(authConfiguration);
        // Use oauth if no existing credentials
        if (azureTokenCredentials == null) {
            final AzureEnvironment environment = AzureEnvironment.AZURE;
            final AzureCredential azureCredential = AzureAuthHelper.oAuthLogin(environment);
            AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
            this.azureTokenCredentials = AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment);
        }

        tracePluginInformation();
        trackMojoExecution(MojoStatus.Start);
    }

    protected void initializeAuthConfiguration() throws MojoFailureException {
        if (!isAuthConfigurationExist()) {
            return;
        }
        if (StringUtils.isNotBlank(auth.getServerId())) {
            if (this.settings.getServer(auth.getServerId()) != null) {
                try {
                    auth = MavenSettingHelper.getAuthConfigurationFromServer(session, settingsDecrypter, auth.getServerId());
                } catch (MavenDecryptException e) {
                    throw new MojoFailureException(e.getMessage());
                }
            } else {
                throw new MojoFailureException(String.format("Unable to get server('%s') from settings.xml.", auth.getServerId()));
            }
        }

        final List<ConfigurationProblem> problems = auth.validate();
        if (problems.stream().anyMatch(problem -> problem.getSeverity() == Severity.ERROR)) {
            throw new MojoFailureException(String.format("Unable to validate auth configuration due to the following errors: %s",
                    problems.stream().map(problem -> problem.getErrorMessage()).collect(Collectors.joining("\n"))));
        }

    }

    protected boolean isAuthConfigurationExist() {
        final String pluginKey = plugin.getPluginLookupKey();
        final Xpp3Dom pluginDom = MavenUtils.getPluginConfiguration(project, pluginKey);
        if (pluginDom == null) {
            return false;
        }
        final Xpp3Dom authDom = pluginDom.getChild("auth");
        return authDom != null && authDom.getChildren().length > 0;
    }

    protected void handleSuccess() {
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_SUCCESS);
        telemetries.put(TELEMETRY_KEY_DURATION, String.valueOf(System.currentTimeMillis() - timeStart));
        trackMojoExecution(MojoStatus.Success);
    }

    protected void handleException(Exception exception) {
        final boolean isUserError = exception instanceof IllegalArgumentException ||
                exception instanceof SpringConfigurationException || exception instanceof InvalidConfigurationException;
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_FAILURE);
        telemetries.put(TELEMETRY_KEY_ERROR_TYPE, isUserError ? TELEMETRY_VALUE_USER_ERROR : TELEMETRY_VALUE_SYSTEM_ERROR);
        telemetries.put(TELEMETRY_KEY_ERROR_MESSAGE, exception.getMessage());
        telemetries.put(TELEMETRY_KEY_DURATION, String.valueOf(System.currentTimeMillis() - timeStart));
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
        telemetries.put(TELEMETRY_KEY_RUNTIME_VERSION, configuration.getRuntimeVersion());
        telemetries.put(TELEMETRY_KEY_CPU, String.valueOf(configuration.getDeployment().getCpu()));
        telemetries.put(TELEMETRY_KEY_MEMORY, String.valueOf(configuration.getDeployment().getMemoryInGB()));
        telemetries.put(TELEMETRY_KEY_INSTANCE_COUNT, String.valueOf(configuration.getDeployment().getInstanceCount()));
        telemetries.put(TELEMETRY_KEY_JVM_OPTIONS,
                String.valueOf(StringUtils.isEmpty(configuration.getDeployment().getJvmOptions())));
    }

    protected void traceAuth() {
        // Todo update deploy mojo telemetries with real value
        telemetries.put(TELEMETRY_KEY_AUTH_METHOD, TELEMETRY_VALUE_AUTH_POM_CONFIGURATION);
        telemetries.put(TELEMETRY_KEY_IS_SERVICE_PRINCIPAL, "false");
        telemetries.put(TELEMETRY_KEY_IS_KEY_ENCRYPTED, "false");
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    public boolean isPublic() {
        return isPublic;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
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

    public SpringServiceClient getSpringServiceClient() {
        if (springServiceClient == null) {
            final LogLevel logLevel = getLog().isDebugEnabled() ? LogLevel.BODY_AND_HEADERS : LogLevel.NONE;
            springServiceClient = new SpringServiceClient(azureTokenCredentials, subscriptionId, logLevel);
        }
        return springServiceClient;
    }
}
