/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSenderFactory;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.GetHashMac;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.auth.AuthConfiguration;
import com.microsoft.azure.maven.auth.AuthenticationSetting;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.auth.AzureAuthHelperLegacy;
import com.microsoft.azure.maven.auth.AzureClientFactory;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.maven.model.SubscriptionOption;
import com.microsoft.azure.maven.telemetry.AppInsightsProxy;
import com.microsoft.azure.maven.telemetry.TelemetryConfiguration;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import com.microsoft.azure.maven.utils.CustomTextIoStringListReader;
import com.microsoft.azure.maven.utils.MavenAuthUtils;
import com.microsoft.azure.maven.utils.ProxyUtils;
import com.microsoft.azure.maven.utils.SystemPropertyUtils;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureLoginException;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base abstract class for all Azure Mojos.
 */
public abstract class AbstractAzureMojo extends AbstractMojo implements TelemetryConfiguration, AuthConfiguration {
    public static final String PLUGIN_NAME_KEY = "pluginName";
    public static final String PLUGIN_VERSION_KEY = "pluginVersion";
    public static final String INSTALLATION_ID_KEY = "installationId";
    public static final String SESSION_ID_KEY = "sessionId";
    public static final String SUBSCRIPTION_ID_KEY = "subscriptionId";
    protected static final String DEPLOY = "deploy";
    private static final String AUTH_TYPE = "authType";
    private static final String AUTH_METHOD = "authMethod";
    private static final String TELEMETRY_NOT_ALLOWED = "TelemetryNotAllowed";
    private static final String INIT_FAILURE = "InitFailure";
    private static final String AZURE_INIT_FAIL = "Failed to authenticate with Azure. Please check your configuration.";
    private static final String FAILURE_REASON = "failureReason";
    private static final String JVM_UP_TIME = "jvmUpTime";
    private static final String CONFIGURATION_PATH = Paths.get(System.getProperty("user.home"),
            ".azure", "mavenplugins.properties").toString();
    private static final String FIRST_RUN_KEY = "first.run";
    private static final String PRIVACY_STATEMENT = "\nData/Telemetry\n" +
            "---------\n" +
            "This project collects usage data and sends it to Microsoft to help improve our products and services.\n" +
            "Read Microsoft's privacy statement to learn more: https://privacy.microsoft.com/en-us/privacystatement." +
            "\n\nYou can change your telemetry configuration through 'allowTelemetry' property.\n" +
            "For more information, please go to https://aka.ms/azure-maven-config.\n";
    protected static final String SUBSCRIPTION_TEMPLATE = "Subscription: %s(%s)";
    protected static final String USING_AZURE_ENVIRONMENT = "Using Azure environment: %s.";
    protected static final String SUBSCRIPTION_NOT_SPECIFIED = "Subscription ID was not specified, using the first subscription in current account," +
            " please refer https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#subscription for more information.";
    protected static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account.";

    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
            "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";

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
     * Authentication setting for Azure Management API.<p>
     * Below are the supported sub-elements within {@code <authentication>}. You can use one of them to authenticate
     * with azure<p>
     * {@code <serverId>} specifies the credentials of your Azure service principal, by referencing a server definition
     * in Maven's settings.xml<p>
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
     * Boolean flag to control whether throwing exception from current Maven plugin when meeting any error.<p>
     * If set to true, the exception from current Maven plugin will fail the current Maven run.
     *
     * @since 0.1.0
     */
    @Parameter(property = "failsOnError", defaultValue = "true")
    protected boolean failsOnError;

    /**
     * Use a HTTP proxy host for the Azure Auth Client
     */
    @Parameter(property = "httpProxyHost")
    protected String httpProxyHost;

    /**
     * Use a HTTP proxy port for the Azure Auth Client
     */
    @Parameter(property = "httpProxyPort")
    protected String httpProxyPort;

    /**
     * Authentication type, could be oauth2, device_code, azure_cli,..., see <code>AuthType</code>
     * If this is not set, maven plugin try all available auth methods with certain order
     *
     * @since 1.2.13
     */
    @Parameter(property = "authType")
    protected String authType;

    @Parameter(property = "auth")
    protected MavenAuthConfiguration auth;

    @Component
    protected SettingsDecrypter settingsDecrypter;

    private Account azureAccount;

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

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public AuthenticationSetting getAuthenticationSetting() {
        return authentication;
    }

    @Override
    public String getSubscriptionId() {
        return StringUtils.firstNonBlank(subscriptionId, (azure == null || azure.getCurrentSubscription() == null) ?
                null : azure.getCurrentSubscription().subscriptionId());
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

    @Override
    public String getUserAgent() {
        return isTelemetryAllowed() ? String.format("%s/%s %s:%s %s:%s", getPluginName(), getPluginVersion(),
                INSTALLATION_ID_KEY, getInstallationId(), SESSION_ID_KEY, getSessionId())
                : String.format("%s/%s", getPluginName(), getPluginVersion());
    }

    @Override
    public String getHttpProxyHost() {
        return httpProxyHost;
    }

    @Override
    public int getHttpProxyPort() {
        return NumberUtils.toInt(httpProxyPort, 0);
    }

    public Azure getAzureClient() throws AzureAuthFailureException, AzureExecutionException {
        if (azure == null) {
            if (this.authentication != null && (this.authentication.getFile() != null || StringUtils.isNotBlank(authentication.getServerId()))) {
                // TODO: remove the old way of authentication
                Log.warn("You are using an old way of authentication which will be deprecated in future versions, please change your configurations.");
                azure = new AzureAuthHelperLegacy(this).getAzureClient();
            } else {
                azure = getOrCreateAzureClient();

            }
            printCurrentSubscription(azure);
            getTelemetryProxy().addDefaultProperty(AUTH_TYPE, getAuthType());
            getTelemetryProxy().addDefaultProperty(AUTH_METHOD, getAuthMethod());
            // Repopulate subscriptionId in case it is not configured.
            getTelemetryProxy().addDefaultProperty(SUBSCRIPTION_ID_KEY, azure.subscriptionId());
        }
        return azure;
    }

    protected String getAuthType() {
        return StringUtils.firstNonBlank(auth == null ? null : auth.getType(), authType);
    }

    protected String selectSubscription(Subscription[] subscriptions) throws AzureExecutionException {
        if (subscriptions.length == 0) {
            throw new AzureExecutionException("Cannot find any subscriptions in current account.");
        }
        if (subscriptions.length == 1) {
            Log.info(String.format("There is only one subscription '%s' in your account, will use it automatically.",
                    TextUtils.blue(SubscriptionOption.getSubscriptionName(subscriptions[0]))));
            return subscriptions[0].getId();
        }
        final List<SubscriptionOption> wrapSubs = Arrays.stream(subscriptions).map(t -> new SubscriptionOption(t))
                .sorted()
                .collect(Collectors.toList());
        final SubscriptionOption defaultValue = wrapSubs.get(0);
        final TextIO textIO = TextIoFactory.getTextIO();
        final SubscriptionOption subscriptionOptionSelected = new CustomTextIoStringListReader<SubscriptionOption>(() -> textIO.getTextTerminal(), null)
                .withCustomPrompt(String.format("Please choose a subscription%s: ",
                        highlightDefaultValue(defaultValue == null ? null : defaultValue.getSubscriptionName())))
                .withNumberedPossibleValues(wrapSubs).withDefaultValue(defaultValue).read("Available subscriptions:");
        if (subscriptionOptionSelected == null) {
            throw new AzureExecutionException("You must select a subscription.");
        }
        return subscriptionOptionSelected.getSubscription().getId();
    }

    protected Azure getOrCreateAzureClient() throws AzureAuthFailureException, AzureExecutionException {
        try {
            final Account account = getAzureAccount();
            final List<Subscription> subscriptions = account.getSubscriptions();
            final String targetSubscriptionId = getTargetSubscriptionId(getSubscriptionId(), subscriptions, account.getSelectedSubscriptions());
            checkSubscription(subscriptions, targetSubscriptionId);

            this.subscriptionId = targetSubscriptionId;
            return AzureClientFactory.getAzureClient(getUserAgent(), targetSubscriptionId);
        } catch (AzureLoginException | IOException e) {
            throw new AzureAuthFailureException(e.getMessage());
        }
    }

    protected Account getAzureAccount() throws MavenDecryptException, AzureExecutionException, LoginFailureException {
        if (azureAccount == null) {
            final MavenAuthConfiguration mavenAuthConfiguration = auth == null ? new MavenAuthConfiguration() : auth;
            mavenAuthConfiguration.setType(getAuthType());

            SystemPropertyUtils.injectCommandLineParameter("auth", mavenAuthConfiguration, MavenAuthConfiguration.class);
            com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).login(
                    MavenAuthUtils.buildAuthConfiguration(session, settingsDecrypter, mavenAuthConfiguration));
            azureAccount = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
            final AzureEnvironment env = azureAccount.getEnvironment();
            final String environmentName = AzureEnvironmentUtils.azureEnvironmentToString(env);
            if (env != AzureEnvironment.AZURE) {
                Log.prompt(String.format(USING_AZURE_ENVIRONMENT, TextUtils.cyan(environmentName)));
            }
            printCredentialDescription(azureAccount);
        }
        return azureAccount;
    }

    protected void printCredentialDescription(Account account) {
        System.out.println(account.toString());
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
    protected static void printCurrentSubscription(Azure azure) {
        if (azure == null) {
            return;
        }
        final com.microsoft.azure.management.resources.Subscription subscription = azure.getCurrentSubscription();
        if (subscription != null) {
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.displayName()), TextUtils.cyan(subscription.subscriptionId())));
        }
    }

    //region Telemetry Configuration Interface

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = new HashMap<>();
        map.put(INSTALLATION_ID_KEY, getInstallationId());
        map.put(PLUGIN_NAME_KEY, getPluginName());
        map.put(PLUGIN_VERSION_KEY, getPluginVersion());
        map.put(SUBSCRIPTION_ID_KEY, getSubscriptionId());
        map.put(SESSION_ID_KEY, getSessionId());
        return map;
    }

    public String getAuthMethod() {
        final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
        if (account != null) {
            return account.getMethod().toString();
        }
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
            // init proxy manager
            ProxyUtils.initProxy(Optional.ofNullable(this.session).map(MavenSession::getRequest).orElse(null));

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
                Log.info("Skip execution.");
                trackMojoSkip();
            } else {
                trackMojoStart();

                doExecute();

                trackMojoSuccess();
            }
        } catch (Exception e) {
            if (e instanceof AzureToolkitAuthenticationException) {
                throw new MojoExecutionException(String.format("Cannot authenticate due to error: %s", e.getMessage()), e);
            }
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
     * @throws AzureExecutionException
     */
    protected abstract void doExecute() throws AzureExecutionException;

    //endregion

    //region Telemetry

    protected void trackMojoSkip() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".skip");
    }

    protected void trackMojoStart() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".start");
    }

    protected void trackMojoSuccess() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".success", recordJvmUpTime(new HashMap<>()));
    }

    protected void trackMojoFailure(final String message) {
        final Map<String, String> failureParameters = new HashMap<>();
        failureParameters.put(FAILURE_REASON, message);
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".failure", recordJvmUpTime(failureParameters));
    }

    //endregion

    //region Helper methods

    protected static String highlightDefaultValue(String defaultValue) {
        return StringUtils.isBlank(defaultValue) ? "" : String.format(" [%s]", TextUtils.blue(defaultValue));
    }

    protected void handleException(final Exception exception) throws MojoExecutionException {
        String message = exception.getMessage();
        if (StringUtils.isEmpty(message)) {
            message = exception.toString();
        }
        trackMojoFailure(message);

        if (isFailingOnError()) {
            throw new MojoExecutionException(message, exception);
        } else {
            Log.error(message);
        }
    }

    protected void executeWithTimeRecorder(RunnableWithException operation, String name) throws AzureExecutionException {
        final long startTime = System.currentTimeMillis();
        try {
            operation.run();
        } catch (Exception e) {
            throw new AzureExecutionException(e.getMessage(), e);
        } finally {
            final long endTime = System.currentTimeMillis();
            getTelemetryProxy().addDefaultProperty(String.format("%s-cost", name), String.valueOf(endTime - startTime));
        }
    }

    private Map<String, String> recordJvmUpTime(Map<String, String> properties) {
        final long jvmUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
        properties.put(JVM_UP_TIME, String.valueOf(jvmUpTime));
        return properties;
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
            Log.debug(e.getMessage());
        }
        return true;
    }

    private void updateConfigurationFile(Properties prop) {
        try (OutputStream output = new FileOutputStream(CONFIGURATION_PATH)) {
            prop.setProperty(FIRST_RUN_KEY, "false");
            prop.store(output, "Azure Maven Plugin configurations");
        } catch (Exception e) {
            // catch exceptions here to avoid blocking mojo execution.
            Log.debug(e.getMessage());
        }
    }

    protected class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.debug("uncaughtException: " + e);
        }
    }

    //endregion

    //region Logging

    public void infoWithMultipleLines(final String messages) {
        final String[] messageArray = messages.split("\\n");
        for (final String line : messageArray) {
            Log.info(line);
        }
    }

    protected interface RunnableWithException {
        void run() throws Exception;
    }
    //endregion

    protected String getTargetSubscriptionId(String defaultSubscriptionId,
                                             List<Subscription> subscriptions,
                                             List<Subscription> selectedSubscriptions) throws IOException, AzureExecutionException {
        String targetSubscriptionId = defaultSubscriptionId;

        if (StringUtils.isBlank(targetSubscriptionId) && selectedSubscriptions.size() == 1) {
            targetSubscriptionId = selectedSubscriptions.get(0).getId();
        }

        if (StringUtils.isBlank(targetSubscriptionId)) {
            return selectSubscription(subscriptions.toArray(new Subscription[0]));
        }
        return targetSubscriptionId;
    }

    protected static void checkSubscription(List<Subscription> subscriptions, String targetSubscriptionId) throws AzureLoginException {
        if (StringUtils.isEmpty(targetSubscriptionId)) {
            Log.warn(SUBSCRIPTION_NOT_SPECIFIED);
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.getId(), targetSubscriptionId))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new AzureLoginException(String.format(SUBSCRIPTION_NOT_FOUND, targetSubscriptionId));
        }
    }

}
