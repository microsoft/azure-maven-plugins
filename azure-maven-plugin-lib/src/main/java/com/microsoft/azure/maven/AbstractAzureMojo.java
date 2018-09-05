/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.auth.AuthConfiguration;
import com.microsoft.azure.maven.auth.AuthenticationSetting;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.auth.AzureAuthHelper;
import com.microsoft.azure.maven.telemetry.AppInsightsProxy;
import com.microsoft.azure.maven.telemetry.GetHashMac;
import com.microsoft.azure.maven.telemetry.TelemetryConfiguration;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Base abstract class for all Azure Mojos.
 */
public abstract class AbstractAzureMojo extends AbstractMojo implements TelemetryConfiguration, AuthConfiguration {
    public static final String PLUGIN_NAME_KEY = "pluginName";
    public static final String PLUGIN_VERSION_KEY = "pluginVersion";
    public static final String INSTALLATION_ID_KEY = "installationId";
    public static final String SESSION_ID_KEY = "sessionId";
    public static final String SUBSCRIPTION_ID_KEY = "subscriptionId";
    public static final String AUTH_TYPE = "authType";
    public static final String TELEMETRY_NOT_ALLOWED = "TelemetryNotAllowed";
    public static final String INIT_FAILURE = "InitFailure";
    public static final String AZURE_INIT_FAIL = "Failed to authenticate with Azure. Please check your configuration.";
    public static final String FAILURE_REASON = "failureReason";
    private static final String CONFIGURATION_PATH = Paths.get(System.getProperty("user.home"),
        ".azure", "mavenplugins.properties").toString();
    private static final String FIRST_RUN_KEY = "first.run";
    private static final String PRIVACY_STATEMENT = "\nData/Telemetry\n" +
        "---------\n" +
        "This project collects usage data and sends it to Microsoft to help improve our products and services.\n" +
        "Read Microsoft's privacy statement to learn more: https://privacy.microsoft.com/en-us/privacystatement." +
        "\n\nYou can change your telemetry configuration through 'allowTelemetry' property.\n" +
        "For more information, please go to https://aka.ms/azure-maven-config.\n";

    //region Properties

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    protected File buildDirectory;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor plugin;

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * Authentication setting for Azure Management API.<br/>
     * Below are the supported sub-elements within {@code <authentication>}. You can use one of them to authenticate
     * with azure<br/>
     * {@code <serverId>} specifies the credentials of your Azure service principal, by referencing a server definition
     * in Maven's settings.xml<br/>
     * {@code <file>} specifies the absolute path of your authentication file for Azure.
     *
     * @since 0.1.0
     */
    @Parameter
    protected AuthenticationSetting authentication;

    /**
     * Azure subscription Id. You only need to specify it when:
     * <ul>
     * <li>you are using authentication file</li>
     * <li>there are more than one subscription in the authentication file</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter
    protected String subscriptionId = "";

    /**
     * Boolean flag to turn on/off telemetry within current Maven plugin.
     *
     * @since 0.1.0
     */
    @Parameter(property = "allowTelemetry", defaultValue = "true")
    protected boolean allowTelemetry;

    /**
     * Boolean flag to control whether throwing exception from current Maven plugin when meeting any error.<br/>
     * If set to true, the exception from current Maven plugin will fail the current Maven run.
     *
     * @since 0.1.0
     */
    @Parameter(property = "failsOnError", defaultValue = "true")
    protected boolean failsOnError;

    /**
     * Use a HTTP proxy host for the Azure Auth Client
     */
    @Parameter(property = "httpProxyHost", readonly = false, required = false)
    protected String httpProxyHost = "";

    /**
     * Use a HTTP proxy port for the Azure Auth Client
     */
    @Parameter(property = "httpProxyPort", readonly = false, required = false)
    protected int httpProxyPort = 80;

    private AzureAuthHelper azureAuthHelper = new AzureAuthHelper(this);

    private Azure azure;

    private TelemetryProxy telemetryProxy;

    private String sessionId = UUID.randomUUID().toString();

    private String installationId = GetHashMac.getHashMac();

    //endregion

    //region Getter

    public MavenProject getProject() {
        return project;
    }

    public MavenSession getSession() {
        return session;
    }

    public String getBuildDirectoryAbsolutePath() {
        return buildDirectory.getAbsolutePath();
    }

    public MavenResourcesFiltering getMavenResourcesFiltering() {
        return mavenResourcesFiltering;
    }

    public Settings getSettings() {
        return settings;
    }

    public AuthenticationSetting getAuthenticationSetting() {
        return authentication;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public boolean isTelemetryAllowed() {
        return allowTelemetry;
    }

    public boolean isFailingOnError() {
        return failsOnError;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstallationId() {
        return installationId == null ? "" : installationId;
    }

    public String getPluginName() {
        return plugin.getArtifactId();
    }

    public String getPluginVersion() {
        return plugin.getVersion();
    }

    public String getUserAgent() {
        return isTelemetryAllowed() ? String.format("%s/%s %s:%s %s:%s", getPluginName(), getPluginVersion(),
                        INSTALLATION_ID_KEY, getInstallationId(), SESSION_ID_KEY, getSessionId())
                : String.format("%s/%s", getPluginName(), getPluginVersion());
    }

    public String getHttpProxyHost() {
        return httpProxyHost;
    }  
    
    public int getHttpProxyPort() {
        return httpProxyPort;
    } 

    public Azure getAzureClient() throws AzureAuthFailureException {
        if (azure == null) {
            // Do something here to check proxy property 
            // @todo 

            azure = azureAuthHelper.getAzureClient();
            if (azure == null) {
                getTelemetryProxy().trackEvent(INIT_FAILURE);
                throw new AzureAuthFailureException(AZURE_INIT_FAIL);
            } else {
                // Repopulate subscriptionId in case it is not configured.
                getTelemetryProxy().addDefaultProperty(SUBSCRIPTION_ID_KEY, azure.subscriptionId());
            }
        }
        return azure;
    }

    public TelemetryProxy getTelemetryProxy() {
        if (telemetryProxy == null) {
            initTelemetry();
        }
        return telemetryProxy;
    }

    protected void initTelemetry() {
        telemetryProxy = new AppInsightsProxy(this);
        if (!isTelemetryAllowed()) {
            telemetryProxy.trackEvent(TELEMETRY_NOT_ALLOWED);
            telemetryProxy.disable();
        }
    }

    //endregion

    //region Telemetry Configuration Interface

    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = new HashMap<>();
        map.put(INSTALLATION_ID_KEY, getInstallationId());
        map.put(PLUGIN_NAME_KEY, getPluginName());
        map.put(PLUGIN_VERSION_KEY, getPluginVersion());
        map.put(SUBSCRIPTION_ID_KEY, getSubscriptionId());
        map.put(SESSION_ID_KEY, getSessionId());
        map.put(AUTH_TYPE, getAuthType());
        return map;
    }

    // TODO:
    // Add AuthType ENUM and move to AzureAuthHelper.
    public String getAuthType() {
        final AuthenticationSetting authSetting = getAuthenticationSetting();
        if (authSetting == null) {
            return "AzureCLI";
        }
        if (StringUtils.isNotEmpty(authSetting.getServerId())) {
            return "ServerId";
        }
        if (authSetting.getFile() != null) {
            return "AuthFile";
        }
        return "Unknown";
    }

    //endregion

    //region Entry Point

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // Work around for Application Insights Java SDK:
            // Sometimes, NoClassDefFoundError will be thrown even after Maven build is completed successfully.
            // An issue has been filed at https://github.com/Microsoft/ApplicationInsights-Java/issues/416
            // Before this issue is fixed, set default uncaught exception handler for all threads as work around.
            Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());

            final Properties prop = new Properties();
            if (isFirstRun(prop)) {
                infoWithMultipleLines(PRIVACY_STATEMENT);
                updateConfigurationFile(prop);
            }

            if (isSkipMojo()) {
                info("Skip execution.");
                trackMojoSkip();
            } else {
                trackMojoStart();

                doExecute();

                trackMojoSuccess();
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Sub-class can override this method to decide whether skip execution.
     *
     * @return Boolean to indicate whether skip execution.
     */
    protected boolean isSkipMojo() {
        return false;
    }

    /**
     * Entry point of sub-class. Sub-class should implement this method to do real work.
     *
     * @throws Exception
     */
    protected abstract void doExecute() throws Exception;

    //endregion

    //region Telemetry

    protected void trackMojoSkip() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".skip");
    }

    protected void trackMojoStart() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".start");
    }

    protected void trackMojoSuccess() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".success");
    }

    protected void trackMojoFailure(final String message) {
        final HashMap<String, String> failureReason = new HashMap<>();
        failureReason.put(FAILURE_REASON, message);
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".failure", failureReason);
    }

    //endregion

    //region Helper methods

    protected void handleException(final Exception exception) throws MojoExecutionException {
        String message = exception.getMessage();
        if (StringUtils.isEmpty(message)) {
            message = exception.toString();
        }
        trackMojoFailure(message);

        if (isFailingOnError()) {
            throw new MojoExecutionException(message, exception);
        } else {
            error(message);
        }
    }

    private boolean isFirstRun(Properties prop) {
        try {
            final File configurationFile = new File(CONFIGURATION_PATH);
            if (configurationFile.exists()) {
                try (InputStream input = new FileInputStream(CONFIGURATION_PATH)) {
                    prop.load(input);
                    final String firstRunValue = prop.getProperty(FIRST_RUN_KEY);
                    if (firstRunValue != null && !firstRunValue.isEmpty() && firstRunValue.equalsIgnoreCase("false")) {
                        return false;
                    }
                }
            } else {
                configurationFile.getParentFile().mkdirs();
                configurationFile.createNewFile();
            }
        } catch (Exception e) {
            // catch exceptions here to avoid blocking mojo execution.
            debug(e.getMessage());
        }
        return true;
    }

    private void updateConfigurationFile(Properties prop) {
        try (OutputStream output = new FileOutputStream(CONFIGURATION_PATH)) {
            prop.setProperty(FIRST_RUN_KEY, "false");
            prop.store(output, "Azure Maven Plugin configurations");
        } catch (Exception e) {
            // catch exceptions here to avoid blocking mojo execution.
            debug(e.getMessage());
        }
    }

    protected class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            debug("uncaughtException: " + e);
        }
    }

    //endregion

    //region Logging

    public void debug(final String message) {
        getLog().debug(message);
    }

    public void info(final String message) {
        getLog().info(message);
    }

    public void infoWithMultipleLines(final String messages) {
        final String[] messageArray = messages.split("\\n");
        for (final String line : messageArray) {
            getLog().info(line);
        }
    }

    public void warning(final String message) {
        getLog().warn(message);
    }

    public void error(final String message) {
        getLog().error(message);
    }

    //endregion
}
