/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.applicationinsights.internal.channel.common.ApacheSenderFactory;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.maven.model.SubscriptionOption;
import com.microsoft.azure.maven.utils.CustomTextIoStringListReader;
import com.microsoft.azure.maven.utils.MavenAuthUtils;
import com.microsoft.azure.maven.utils.ProxyUtils;
import com.microsoft.azure.maven.utils.SystemPropertyUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureLoginException;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.maven.common.messager.MavenAzureMessager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
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
public abstract class AbstractAzureMojo extends AbstractMojo {
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
    private static final String ERROR_MESSAGE = "error.message";
    private static final String ERROR_STACK = "error.stack";
    private static final String ERROR_CLASSNAME = "error.class_name";
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
    protected static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account.";

    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
            "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";
    private static final String AZURE_ENVIRONMENT = "azureEnvironment";
    private static final String PROXY = "proxy";

    //region Properties

    @Getter
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Getter
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
    @Getter
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Getter
    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

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
    @Getter
    protected String subscriptionId = "";

    /**
     * Boolean flag to turn on/off telemetry within current Maven plugin.
     *
     * @since 0.1.0
     */
    @Getter
    @Parameter(property = "allowTelemetry", defaultValue = "true")
    protected boolean allowTelemetry;

    /**
     * Boolean flag to control whether throwing exception from current Maven plugin when meeting any error.<p>
     * If set to true, the exception from current Maven plugin will fail the current Maven run.
     *
     * @since 0.1.0
     */
    @Getter
    @Parameter(property = "failsOnError", defaultValue = "true")
    protected boolean failsOnError;

    /**
     * Use a HTTP proxy host for the Azure Auth Client
     */
    @Parameter(property = "httpProxyHost")
    @Getter
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

    private com.microsoft.azure.management.Azure azure;

    @Getter
    protected AzureTelemetryClient telemetryProxy;
    @Getter
    protected Map<String, String> telemetries = new HashMap<>();

    @Getter
    private final String sessionId = UUID.randomUUID().toString();

    private final String installationId = InstallationIdUtils.getHashMac();

    //endregion

    public String getBuildDirectoryAbsolutePath() {
        return buildDirectory.getAbsolutePath();
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
        return isAllowTelemetry() ? String.format("%s/%s %s:%s %s:%s", getPluginName(), getPluginVersion(),
                INSTALLATION_ID_KEY, getInstallationId(), SESSION_ID_KEY, getSessionId())
                : String.format("%s/%s", getPluginName(), getPluginVersion());
    }

    public int getHttpProxyPort() {
        return NumberUtils.toInt(httpProxyPort, 0);
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
        final List<SubscriptionOption> wrapSubs = Arrays.stream(subscriptions).map(SubscriptionOption::new)
                .sorted()
                .collect(Collectors.toList());
        final SubscriptionOption defaultValue = wrapSubs.get(0);
        final TextIO textIO = TextIoFactory.getTextIO();
        final SubscriptionOption subscriptionOptionSelected = new CustomTextIoStringListReader<SubscriptionOption>(textIO::getTextTerminal, null)
                .withCustomPrompt(String.format("Please choose a subscription%s: ",
                        highlightDefaultValue(defaultValue == null ? null : defaultValue.getSubscriptionName())))
                .withNumberedPossibleValues(wrapSubs).withDefaultValue(defaultValue).read("Available subscriptions:");
        if (subscriptionOptionSelected == null) {
            throw new AzureExecutionException("You must select a subscription.");
        }
        return subscriptionOptionSelected.getSubscription().getId();
    }

    protected Account getAzureAccount() throws MavenDecryptException, AzureExecutionException, LoginFailureException {
        if (azureAccount == null) {
            final MavenAuthConfiguration mavenAuthConfiguration = auth == null ? new MavenAuthConfiguration() : auth;
            mavenAuthConfiguration.setType(getAuthType());

            SystemPropertyUtils.injectCommandLineParameter("auth", mavenAuthConfiguration, MavenAuthConfiguration.class);
            Azure.az().config().setUserAgent(getUserAgent());
            azureAccount = login(MavenAuthUtils.buildAuthConfiguration(session, settingsDecrypter, mavenAuthConfiguration));
        }
        return azureAccount;
    }

    @SneakyThrows
    protected void selectSubscription() {
        final Account account = Azure.az(AzureAccount.class).account();
        final List<Subscription> subscriptions = account.getSubscriptions();
        final String targetSubscriptionId = getTargetSubscriptionId(getSubscriptionId(), subscriptions, account.getSelectedSubscriptions());
        checkSubscription(subscriptions, targetSubscriptionId);
        account.selectSubscription(Collections.singletonList(targetSubscriptionId));
        final Subscription subscription = account.getSubscription(targetSubscriptionId);
        Log.info(String.format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.getName()), TextUtils.cyan(subscription.getId())));
        this.subscriptionId = targetSubscriptionId;
    }

    protected Account login(@Nonnull com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {
        promptAzureEnvironment(auth.getEnvironment());
        MavenAuthUtils.disableIdentityLogs();
        accountLogin(auth);
        final Account account = Azure.az(AzureAccount.class).account();
        final boolean isInteractiveLogin = account.getAuthType() == AuthType.OAUTH2 || account.getAuthType() == AuthType.DEVICE_CODE;
        final AzureEnvironment env = account.getEnvironment();
        final String environmentName = AzureEnvironmentUtils.azureEnvironmentToString(env);
        if (env != AzureEnvironment.AZURE && env != auth.getEnvironment()) {
            Log.prompt(String.format(USING_AZURE_ENVIRONMENT, TextUtils.cyan(environmentName)));
        }
        printCredentialDescription(account, isInteractiveLogin);
        telemetryProxy.addDefaultProperty(AUTH_TYPE, getAuthType());
        telemetryProxy.addDefaultProperty(AUTH_METHOD, getActualAuthType());
        telemetryProxy.addDefaultProperty(AZURE_ENVIRONMENT, environmentName);
        return account;
    }

    private static Account accountLogin(@Nonnull com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {

        if (auth.getEnvironment() != null) {
            Azure.az(AzureCloud.class).set(auth.getEnvironment());
        }
        // handle null type
        if (auth.getType() == null || auth.getType() == AuthType.AUTO) {
            if (StringUtils.isAllBlank(auth.getCertificate(), auth.getCertificatePassword(), auth.getKey())) {
                // not service principal configuration, will list accounts and try them one by one
                final Account account = findFirstAvailableAccount().block();
                if (account == null) {
                    throw new AzureToolkitAuthenticationException("There are no accounts available.");
                }
                // prompt if oauth or device code
                promptForOAuthOrDeviceCodeLogin(account.getAuthType());
                return handleDeviceCodeAccount(Azure.az(AzureAccount.class).loginAsync(account, false).block());
            } else {
                // user specify SP related configurations
                return doServicePrincipalLogin(auth);
            }
        } else {
            // user specifies the auth type explicitly
            promptForOAuthOrDeviceCodeLogin(auth.getType());
            return handleDeviceCodeAccount(Azure.az(AzureAccount.class).loginAsync(auth, false).block());
        }
    }

    private static Account handleDeviceCodeAccount(Account account) {
        if (account instanceof DeviceCodeAccount) {
            final DeviceCodeAccount deviceCodeAccount = (DeviceCodeAccount) account;
            final DeviceCodeInfo challenge = deviceCodeAccount.getDeviceCode();
            System.out.println(StringUtils.replace(challenge.getMessage(), challenge.getUserCode(),
                    TextUtils.cyan(challenge.getUserCode())));
        }
        return account.continueLogin().block();
    }

    private static void promptAzureEnvironment(AzureEnvironment env) {
        if (env != null && env != AzureEnvironment.AZURE) {
            Log.prompt(String.format("Auth environment: %s", TextUtils.cyan(AzureEnvironmentUtils.azureEnvironmentToString(env))));
        }
    }

    private static void promptForOAuthOrDeviceCodeLogin(AuthType authType) {
        if (authType == AuthType.OAUTH2 || authType == AuthType.DEVICE_CODE) {
            Log.prompt(String.format("Auth type: %s", TextUtils.cyan(authType.toString())));
        }
    }

    private static Mono<Account> findFirstAvailableAccount() {
        final List<Account> accounts = Azure.az(AzureAccount.class).accounts();
        if (accounts.isEmpty()) {
            return Mono.error(new AzureToolkitAuthenticationException("There are no accounts available."));
        }
        Mono<Account> current = checkAccountAvailable(accounts.get(0));
        for (int i = 1; i < accounts.size(); i++) {
            final Account ac = accounts.get(i);
            current = current.onErrorResume(e -> checkAccountAvailable(ac));
        }
        return current;
    }

    private static Account doServicePrincipalLogin(com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {
        auth.setType(AuthType.SERVICE_PRINCIPAL);
        return Azure.az(AzureAccount.class).login(auth).account();
    }

    private static Mono<Account> checkAccountAvailable(Account account) {
        return account.checkAvailable().map(avail -> {
            if (avail) {
                return account;
            }
            throw new AzureToolkitAuthenticationException(String.format("Cannot login with auth type: %s", account.getAuthType()));
        });
    }

    protected static void printCredentialDescription(Account account, boolean skipType) {
        if (skipType) {
            if (CollectionUtils.isNotEmpty(account.getSubscriptions())) {
                final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
                if (selectedSubscriptions != null && selectedSubscriptions.size() == 1) {
                    System.out.printf("Default subscription: %s(%s)%n", TextUtils.cyan(selectedSubscriptions.get(0).getName()),
                            TextUtils.cyan(selectedSubscriptions.get(0).getId()));
                }
            }

            if (StringUtils.isNotEmpty(account.getEntity().getEmail())) {
                System.out.printf("Username: %s%n", TextUtils.cyan(account.getEntity().getEmail()));
            }
        } else {
            System.out.println(account.toString());
        }
    }

    protected void initTelemetryProxy() {
        final Map<String, String> properties = getTelemetryProperties();
        telemetryProxy = new AzureTelemetryClient(properties);
        AzureTelemeter.setClient(telemetryProxy);
        AzureTelemeter.setEventNamePrefix("AzurePlugin.Maven");
        if (!isAllowTelemetry()) {
            telemetryProxy.trackEvent(TELEMETRY_NOT_ALLOWED);
            telemetryProxy.disable();
        }
    }

    //endregion
    protected static void printCurrentSubscription(com.microsoft.azure.management.Azure azure) {
        if (azure == null) {
            return;
        }
        final com.microsoft.azure.management.resources.Subscription subscription = azure.getCurrentSubscription();
        if (subscription != null) {
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.displayName()), TextUtils.cyan(subscription.subscriptionId())));
        }
    }

    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> map = new HashMap<>();
        map.put(INSTALLATION_ID_KEY, getInstallationId());
        map.put(PLUGIN_NAME_KEY, getPluginName());
        map.put(PLUGIN_VERSION_KEY, getPluginVersion());
        map.put(SUBSCRIPTION_ID_KEY, getSubscriptionId());
        map.put(SESSION_ID_KEY, getSessionId());
        return map;
    }

    public String getActualAuthType() {
        final Account account = Azure.az(AzureAccount.class).account();
        if (account != null) {
            return account.getAuthType().toString();
        }
        return "Unknown";
    }

    //region Entry Point

    @Override
    public void execute() throws MojoExecutionException {
        try {
            AzureMessager.setDefaultMessager(new MavenAzureMessager());
            Azure.az().config().setLogLevel(HttpLogDetailLevel.NONE.name());
            Azure.az().config().setUserAgent(getUserAgent());
            // init proxy manager
            ProxyUtils.initProxy(Optional.ofNullable(this.session).map(MavenSession::getRequest).orElse(null));
            initTelemetryProxy();
            telemetryProxy.addDefaultProperty(PROXY, String.valueOf(ProxyManager.getInstance().getProxy() != null));
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
                onSkipped();
            } else {
                beforeMojoExecution();

                doExecute();

                afterMojoExecution();
            }
        } catch (Exception e) {
            onMojoError(e);
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
     */
    protected abstract void doExecute() throws AzureExecutionException;

    protected void onSkipped() {
        telemetryProxy.trackEvent(this.getClass().getSimpleName() + ".skip");
    }

    protected void beforeMojoExecution() {
        telemetryProxy.trackEvent(this.getClass().getSimpleName() + ".start", this.getTelemetries(), null, false);
    }

    protected void afterMojoExecution() {
        telemetryProxy.trackEvent(this.getClass().getSimpleName() + ".success", recordJvmUpTime(new HashMap<>()));
    }

    protected void trackMojoFailure(final Throwable throwable) {
        final Map<String, String> failureParameters = new HashMap<>();
        failureParameters.put(ERROR_MESSAGE, throwable.getMessage());
        failureParameters.put(ERROR_STACK, ExceptionUtils.getStackTrace(throwable));
        failureParameters.put(ERROR_CLASSNAME, throwable.getClass().getName());

        telemetryProxy.trackEvent(this.getClass().getSimpleName() + ".failure", recordJvmUpTime(failureParameters));
    }

    protected static String highlightDefaultValue(String defaultValue) {
        return StringUtils.isBlank(defaultValue) ? "" : String.format(" [%s]", TextUtils.blue(defaultValue));
    }

    protected void onMojoError(final Exception exception) throws MojoExecutionException {
        trackMojoFailure(exception);

        final String message = StringUtils.isEmpty(exception.getMessage()) ? exception.toString() : exception.getMessage();
        if (isFailsOnError()) {
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
            telemetryProxy.addDefaultProperty(String.format("%s-cost", name), String.valueOf(endTime - startTime));
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

    protected static class DefaultUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
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
        telemetryProxy.addDefaultProperty(SUBSCRIPTION_ID_KEY, targetSubscriptionId);
        return targetSubscriptionId;
    }

    protected static void checkSubscription(List<Subscription> subscriptions, String targetSubscriptionId) throws AzureLoginException {
        if (StringUtils.isEmpty(targetSubscriptionId)) {
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
