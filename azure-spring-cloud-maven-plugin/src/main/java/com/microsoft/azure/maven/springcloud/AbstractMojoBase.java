/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.LoginFailureException;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.maven.springcloud.config.AppDeploymentMavenConfig;
import com.microsoft.azure.maven.springcloud.config.ConfigurationParser;
import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import com.microsoft.azure.maven.telemetry.MojoStatus;
import com.microsoft.azure.maven.utils.MavenAuthUtils;
import com.microsoft.azure.maven.utils.ProxyUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.tools.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.rest.LogLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_AUTH_METHOD;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_CPU;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_DURATION;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_ERROR_CODE;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_ERROR_MESSAGE;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_ERROR_TYPE;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_INSTANCE_COUNT;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_DEPLOYMENT;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_IS_CREATE_NEW_APP;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_JAVA_VERSION;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_JVM_OPTIONS;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_MEMORY;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_PLUGIN_NAME;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_PLUGIN_VERSION;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_PUBLIC;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_RUNTIME_VERSION;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_SUBSCRIPTION_ID;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_KEY_WITHIN_PARENT_POM;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_VALUE_AUTH_POM_CONFIGURATION;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_VALUE_ERROR_CODE_FAILURE;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_VALUE_ERROR_CODE_SUCCESS;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_VALUE_SYSTEM_ERROR;
import static com.microsoft.azure.maven.springcloud.TelemetryConstants.TELEMETRY_VALUE_USER_ERROR;

public abstract class AbstractMojoBase extends AbstractMojo {
    private static final String INIT_FAILURE = "InitFailure";
    private static final String AZURE_ENVIRONMENT = "azureEnvironment";
    private static final String AZURE_INIT_FAIL = "Failed to authenticate with Azure. Please check your configuration.";
    private static final String USING_AZURE_ENVIRONMENT = "Using Azure environment: %s.";
    private static final String PROXY = "proxy";
    private static final String AUTH_TYPE = "authType";

    @Parameter(property = "auth")
    protected MavenAuthConfiguration auth;

    @Getter
    @Parameter(alias = "public")
    protected Boolean isPublic;

    @Parameter(property = "isTelemetryAllowed", defaultValue = "true")
    protected boolean isTelemetryAllowed;

    @Getter
    @Parameter(property = "subscriptionId")
    protected String subscriptionId;

    @Getter
    @Parameter(property = "clusterName")
    protected String clusterName;

    @Getter
    @Parameter(property = "appName")
    protected String appName;

    @Getter
    @Parameter(property = "runtimeVersion")
    protected String runtimeVersion;

    @Getter
    @Parameter(property = "deployment")
    protected AppDeploymentMavenConfig deployment;

    @Getter
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Getter
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Getter
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    protected File buildDirectory;

    @Getter
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor plugin;

    @Getter
    protected Map<String, String> telemetries;

    @Component
    protected SettingsDecrypter settingsDecrypter;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    protected Long timeStart;
    private AppPlatformManager manager;

    @Parameter(property = "authType")
    protected String authType;

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

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            initExecution();
            doExecute();
            handleSuccess();
        } catch (Exception e) {
            if (e instanceof AzureToolkitAuthenticationException) {
                throw new MojoExecutionException(String.format("Cannot authenticate due to error: %s", e.getMessage()), e);
            }
            handleException(e);
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected void initExecution() throws MavenDecryptException, AzureExecutionException, LoginFailureException {
        // init proxy manager
        ProxyUtils.initProxy(Optional.ofNullable(this.session).map(MavenSession::getRequest).orElse(null));
        // Init telemetries
        initTelemetry();
        telemetries.put(PROXY, String.valueOf(ProxyManager.getInstance().getProxy() != null));
        Azure.az().config().setLogLevel(LogLevel.NONE);
        Azure.az().config().setUserAgent(getUserAgent());
        trackMojoExecution(MojoStatus.Start);
        final MavenAuthConfiguration mavenAuthConfiguration = auth == null ? new MavenAuthConfiguration() : auth;
        login(MavenAuthUtils.buildAuthConfiguration(session, settingsDecrypter, mavenAuthConfiguration));
    }

    private Account login(@Nonnull com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {
        promptAzureEnvironment(auth.getEnvironment());
        accountLogin(auth);
        final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
        if (account.getAuthType() == AuthType.OAUTH2 || account.getAuthType() == AuthType.DEVICE_CODE) {
            if (account.getAuthType() == AuthType.DEVICE_CODE) {
                handleDeviceCodeAccount(account);
            }
        }
        final AzureEnvironment env = account.getEnvironment();
        final String environmentName = AzureEnvironmentUtils.azureEnvironmentToString(env);
        if (env != AzureEnvironment.AZURE && env != auth.getEnvironment()) {
            Log.prompt(String.format(USING_AZURE_ENVIRONMENT, TextUtils.cyan(environmentName)));
        }
        printCredentialDescription(account);
        telemetries.put(AUTH_TYPE, getAuthType());
        telemetries.put(AZURE_ENVIRONMENT, environmentName);
        return account;
    }

    private static Account accountLogin(@Nonnull com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {

        if (auth.getEnvironment() != null) {
            com.microsoft.azure.toolkit.lib.Azure.az(AzureCloud.class).set(auth.getEnvironment());
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
                return handleDeviceCodeAccount(com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).login(account).account());
            } else {
                // user specify SP related configurations
                return doServicePrincipalLogin(auth);
            }
        } else {
            // user specifies the auth type explicitly
            promptForOAuthOrDeviceCodeLogin(auth.getType());
            return handleDeviceCodeAccount(com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).login(auth).account());
        }
    }

    private static Account handleDeviceCodeAccount(Account account) {
        if (account instanceof DeviceCodeAccount) {
            final DeviceCodeAccount deviceCodeAccount = (DeviceCodeAccount) account;
            final DeviceCodeInfo challenge = deviceCodeAccount.getDeviceCode();
            System.out.println(StringUtils.replace(challenge.getMessage(), challenge.getUserCode(),
                    TextUtils.cyan(challenge.getUserCode())));
            return deviceCodeAccount.continueLogin().block();
        } else {
            return account;
        }
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
        final List<Account> accounts = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).accounts();
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
        return com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).login(auth).account();
    }

    private static Mono<Account> checkAccountAvailable(Account account) {
        return account.checkAvailable().map(avail -> {
            if (avail) {
                return account;
            }
            throw new AzureToolkitAuthenticationException(String.format("Cannot login with auth type: %s", account.getAuthType()));
        });
    }

    private static void printCredentialDescription(Account account) {
        System.out.println(account.toString());
    }

    protected String getAuthType() {
        return StringUtils.firstNonBlank(auth == null ? null : auth.getType(), authType);
    }

    protected void initTelemetry() {
        timeStart = System.currentTimeMillis();
        telemetries = new HashMap<>();
        if (!isTelemetryAllowed) {
            AppInsightHelper.INSTANCE.disable();
        }
        tracePluginInformation();
        traceAuth();
        traceConfiguration(this.getConfiguration());
    }

    protected void handleSuccess() {
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_SUCCESS);
        telemetries.put(TELEMETRY_KEY_DURATION, String.valueOf(System.currentTimeMillis() - timeStart));
        trackMojoExecution(MojoStatus.Success);
    }

    protected void handleException(Exception exception) {
        final boolean isUserError = exception instanceof IllegalArgumentException ||
                exception instanceof InvalidConfigurationException;
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_FAILURE);
        telemetries.put(TELEMETRY_KEY_ERROR_TYPE, isUserError ? TELEMETRY_VALUE_USER_ERROR : TELEMETRY_VALUE_SYSTEM_ERROR);
        telemetries.put(TELEMETRY_KEY_ERROR_MESSAGE, exception.getMessage());
        telemetries.put(TELEMETRY_KEY_DURATION, String.valueOf(System.currentTimeMillis() - timeStart));
        trackMojoExecution(MojoStatus.Failure);
    }

    protected void trackMojoExecution(MojoStatus status) {
        final String eventName = String.format("%s.%s", this.getClass().getSimpleName(), status.name());
        AppInsightHelper.INSTANCE.trackEvent(eventName, getTelemetries(), false);
    }

    protected void tracePluginInformation() {
        final String javaVersion = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        telemetries.put(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetries.put(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
        telemetries.put(TELEMETRY_KEY_WITHIN_PARENT_POM, String.valueOf(project.getPackaging().equalsIgnoreCase("pom")));
        telemetries.put(TELEMETRY_KEY_JAVA_VERSION, javaVersion);
    }

    protected void traceConfiguration(SpringCloudAppConfig configuration) {
        telemetries.put(TELEMETRY_KEY_PUBLIC, String.valueOf(configuration.isPublic()));
        telemetries.put(TELEMETRY_KEY_RUNTIME_VERSION, configuration.getRuntimeVersion());
        telemetries.put(TELEMETRY_KEY_CPU, String.valueOf(configuration.getDeployment().getCpu()));
        telemetries.put(TELEMETRY_KEY_MEMORY, String.valueOf(configuration.getDeployment().getMemoryInGB()));
        telemetries.put(TELEMETRY_KEY_INSTANCE_COUNT, String.valueOf(configuration.getDeployment().getInstanceCount()));
        telemetries.put(TELEMETRY_KEY_JVM_OPTIONS,
                String.valueOf(StringUtils.isEmpty(configuration.getDeployment().getJvmOptions())));
        telemetries.put(TELEMETRY_KEY_SUBSCRIPTION_ID, configuration.getSubscriptionId());
    }

    protected void traceAuth() {
        // Todo update deploy mojo telemetries with real value
        telemetries.put(TELEMETRY_KEY_AUTH_METHOD, TELEMETRY_VALUE_AUTH_POM_CONFIGURATION);
    }

    protected void traceDeployment(boolean newApp, boolean newDeployment, SpringCloudAppConfig configuration) {
        final boolean isDeploymentNameGiven = configuration.getDeployment() != null &&
            StringUtils.isNotEmpty(configuration.getDeployment().getDeploymentName());
        telemetries.put(TELEMETRY_KEY_IS_CREATE_NEW_APP, String.valueOf(newApp));
        telemetries.put(TELEMETRY_KEY_IS_CREATE_DEPLOYMENT, String.valueOf(newDeployment));
        telemetries.put(TELEMETRY_KEY_IS_DEPLOYMENT_NAME_GIVEN, String.valueOf(isDeploymentNameGiven));
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException, AzureExecutionException;

    public SpringCloudAppConfig getConfiguration() {
        final ConfigurationParser parser = ConfigurationParser.getInstance();
        return parser.parse(this);
    }

    private String getUserAgent() {
        return isTelemetryAllowed ? String.format("%s/%s installationId:%s sessionId:%s", plugin.getArtifactId(), plugin.getVersion(),
                AppInsightHelper.INSTANCE.getInstallationId(), AppInsightHelper.INSTANCE.getSessionId())
                : String.format("%s/%s", plugin.getArtifactId(), plugin.getVersion());
    }

    protected static String highlightDefaultValue(String defaultValue) {
        return StringUtils.isBlank(defaultValue) ? "" : String.format(" [%s]", TextUtils.blue(defaultValue));
    }
}
