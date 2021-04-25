/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import com.azure.core.management.AzureEnvironment;
import com.azure.identity.DeviceCodeInfo;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.maven.exception.MavenDecryptException;
import com.microsoft.azure.maven.model.MavenAuthConfiguration;
import com.microsoft.azure.maven.model.SubscriptionOption;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.util.ValidationUtil;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.maven.auth.MavenSettingHelper.buildAuthConfigurationByServerId;

public class MavenAuthUtils {
    private static final String INVALID_AZURE_ENVIRONMENT = "Invalid environment string '%s', please replace it with one of " +
            "\"Azure\", \"AzureChina\", \"AzureGermany\", \"AzureUSGovernment\",.";

    private static final String SUBSCRIPTION_NOT_SPECIFIED = "Subscription ID was not specified, using the first subscription in current account," +
            " please refer https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#subscription for more information.";

    private static final String SUBSCRIPTION_NOT_FOUND = "Subscription(%s) was not found in current account.";

    private static final String USING_AZURE_ENVIRONMENT = "Using Azure environment: %s.";

    private static final String SUBSCRIPTION_TEMPLATE = "Subscription: %s(%s)";

    public static AuthConfiguration buildAuthConfiguration(MavenSession session, SettingsDecrypter settingsDecrypter, @Nonnull MavenAuthConfiguration auth)
            throws AzureExecutionException, MavenDecryptException {
        final String serverId = auth.getServerId();
        final AuthConfiguration authConfiguration;
        try {
            authConfiguration = convertToAuthConfiguration(StringUtils.isNotBlank(auth.getServerId()) ?
                    buildAuthConfigurationByServerId(session, settingsDecrypter, serverId) : auth);
        } catch (InvalidConfigurationException ex) {
            final String messagePostfix = StringUtils.isNotBlank(serverId) ? ("in server: '" + serverId + "' at maven settings.xml.")
                    : "in <auth> configuration.";
            throw new AzureExecutionException(String.format("%s %s", ex.getMessage(), messagePostfix));
        }
        return authConfiguration;
    }

    private static AuthConfiguration convertToAuthConfiguration(MavenAuthConfiguration mavenAuthConfiguration)
            throws InvalidConfigurationException {
        if (Objects.isNull(mavenAuthConfiguration)) {
            return new AuthConfiguration();
        }
        final AuthConfiguration authConfiguration = new AuthConfiguration();
        authConfiguration.setClient(mavenAuthConfiguration.getClient());
        authConfiguration.setTenant(mavenAuthConfiguration.getTenant());
        authConfiguration.setCertificate(mavenAuthConfiguration.getCertificate());
        authConfiguration.setCertificatePassword(mavenAuthConfiguration.getCertificatePassword());
        authConfiguration.setKey(mavenAuthConfiguration.getKey());

        final String authTypeStr = mavenAuthConfiguration.getType();
        authConfiguration.setType(AuthType.parseAuthType(authTypeStr));

        authConfiguration.setEnvironment(AzureEnvironmentUtils.stringToAzureEnvironment(mavenAuthConfiguration.getEnvironment()));
        if (StringUtils.isNotBlank(mavenAuthConfiguration.getEnvironment()) && Objects.isNull(authConfiguration.getEnvironment())) {
            throw new InvalidConfigurationException(String.format(INVALID_AZURE_ENVIRONMENT, mavenAuthConfiguration.getEnvironment()));
        }

        // if user specify 'auto', and there are SP configuration errors, it will fail back to other auth types
        // if user doesn't specify any authType
        if (StringUtils.isBlank(mavenAuthConfiguration.getType())) {
            if (!StringUtils.isAllBlank(mavenAuthConfiguration.getCertificate(), mavenAuthConfiguration.getKey(),
                    mavenAuthConfiguration.getCertificatePassword())) {
                ValidationUtil.validateAuthConfiguration(authConfiguration);
            }
        } else if (authConfiguration.getType() == AuthType.SERVICE_PRINCIPAL) {
            ValidationUtil.validateAuthConfiguration(authConfiguration);
        }

        return authConfiguration;
    }

    public static void disableIdentityLogs() {
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.identity", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.adal4j", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.azure.core.credential", "off");
        putPropertyIfNotExist("org.slf4j.simpleLogger.log.com.microsoft.aad.msal4jextensions", "off");
    }

    private static void putPropertyIfNotExist(String key, String value) {
        if (StringUtils.isBlank(System.getProperty(key))) {
            System.setProperty(key, value);
        }
    }

    public static Account login(@Nonnull com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration auth) {
        promptAzureEnvironment(auth.getEnvironment());
        MavenAuthUtils.disableIdentityLogs();
        accountLogin(auth);
        final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
        final boolean isInteractiveLogin = account.getAuthType() == AuthType.OAUTH2 || account.getAuthType() == AuthType.DEVICE_CODE;
        final AzureEnvironment env = account.getEnvironment();
        final String environmentName = AzureEnvironmentUtils.azureEnvironmentToString(env);
        if (env != AzureEnvironment.AZURE && env != auth.getEnvironment()) {
            Log.prompt(String.format(USING_AZURE_ENVIRONMENT, TextUtils.cyan(environmentName)));
        }
        printCredentialDescription(account, isInteractiveLogin);
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
                return handleDeviceCodeAccount(com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).loginAsync(account, false).block());
            } else {
                // user specify SP related configurations
                return doServicePrincipalLogin(auth);
            }
        } else {
            // user specifies the auth type explicitly
            promptForOAuthOrDeviceCodeLogin(auth.getType());
            return handleDeviceCodeAccount(com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).loginAsync(auth, false).block());
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

    private static void printCredentialDescription(Account account, boolean skipType) {
        if (skipType) {
            if (CollectionUtils.isNotEmpty(account.getSubscriptions())) {
                final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
                if (selectedSubscriptions != null && selectedSubscriptions.size() == 1) {
                    System.out.println(String.format("Default subscription: %s(%s)", TextUtils.cyan(selectedSubscriptions.get(0).getName()),
                            TextUtils.cyan(selectedSubscriptions.get(0).getId())));
                }
            }

            if (StringUtils.isNotEmpty(account.getEntity().getEmail())) {
                System.out.println(String.format("Username: %s", TextUtils.cyan(account.getEntity().getEmail())));
            }
        } else {
            System.out.println(account.toString());
        }
    }

    public static String getTargetSubscriptionId(String defaultSubscriptionId,
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

    public static void checkSubscription(List<Subscription> subscriptions, String targetSubscriptionId) {
        if (StringUtils.isEmpty(targetSubscriptionId)) {
            Log.warn(SUBSCRIPTION_NOT_SPECIFIED);
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.getId(), targetSubscriptionId))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new AzureToolkitAuthenticationException(String.format(SUBSCRIPTION_NOT_FOUND, targetSubscriptionId));
        }

        final Account account = Azure.az(AzureAccount.class).account();
        account.selectSubscription(Collections.singletonList(targetSubscriptionId));
        printCurrentSubscription(account);
    }

    public static void printCurrentSubscription(Account account) {
        if (account == null) {
            return;
        }
        final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
        if (selectedSubscriptions != null && selectedSubscriptions.size() == 1) {
            final Subscription subscription = selectedSubscriptions.get(0);
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.getName()), TextUtils.cyan(subscription.getId())));
        }
    }

    private static String selectSubscription(Subscription[] subscriptions) throws AzureExecutionException {
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
                        MavenUtils.highlightDefaultValue(defaultValue == null ? null : defaultValue.getSubscriptionName())))
                .withNumberedPossibleValues(wrapSubs).withDefaultValue(defaultValue).read("Available subscriptions:");
        if (subscriptionOptionSelected == null) {
            throw new AzureExecutionException("You must select a subscription.");
        }
        return subscriptionOptionSelected.getSubscription().getId();
    }

}
