/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.AzureTokenCredentialsDecorator;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.configuration.AuthType;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


public class AzureClientFactory {

    private static final String SUBSCRIPTION_TEMPLATE = "Subscription : %s(%s)";
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account";
    public static final String NO_AVAILABLE_SUBSCRIPTION = "No available subscription found in current account";

    public static Azure getAzureClient(AuthConfiguration auth, String subscriptionId) throws InvalidConfigurationException, IOException,
        AzureLoginFailureException, InterruptedException, ExecutionException {
        AzureTokenCredentialsDecorator azureTokenCredentials = AzureAuthHelper.getAzureTokenCredentials(auth);
        if (azureTokenCredentials == null) {
            AuthType authType;
            final AzureEnvironment environment = AzureEnvironment.AZURE;
            AzureCredential azureCredential;
            try {
                authType = AuthType.OAUTH;
                azureCredential = AzureAuthHelper.oAuthLogin(environment);
            } catch (DesktopNotSupportedException e) {
                authType = AuthType.DEVICE_LOGIN;
                azureCredential = AzureAuthHelper.deviceLogin(environment);
            }
            AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
            azureTokenCredentials = new AzureTokenCredentialsDecorator(authType,
                    AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment));
        }

        if (azureTokenCredentials != null) {
            // Todo: change to united log
            Log.info(azureTokenCredentials.getCredentialDescription());
            final Authenticated authenticated = Azure.configure().authenticate(azureTokenCredentials);
            // For cloud shell, use subscription in profile as the default subscription.
            if (StringUtils.isEmpty(subscriptionId) && AzureAuthHelperLegacy.isInCloudShell()) {
                subscriptionId = AzureAuthHelperLegacy.getSubscriptionOfCloudShell();
            }
            final Azure azure = StringUtils.isEmpty(subscriptionId) ?
                    authenticated.withDefaultSubscription() :
                    authenticated.withSubscription(subscriptionId);
            checkSubscription(azure, subscriptionId);
            final Subscription subscription = azure.getCurrentSubscription();
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, subscription.displayName(), subscription.subscriptionId()));
            return azure;
        }

        return null;
    }

    private static void checkSubscription(Azure azure, String subscriptionId) throws AzureLoginFailureException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        subscriptions.loadAll();
        if (subscriptions.size() == 0) {
            throw new AzureLoginFailureException(NO_AVAILABLE_SUBSCRIPTION);
        }
        final Optional<Subscription> targetSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.subscriptionId(), subscriptionId))
                .findAny();
        if (!targetSubscription.isPresent()) {
            throw new AzureLoginFailureException(String.format(SUBSCRIPTION_NOT_FOUND, subscriptionId));
        }
    }
}
