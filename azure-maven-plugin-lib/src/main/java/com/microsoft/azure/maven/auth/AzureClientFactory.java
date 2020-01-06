/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.configuration.AuthType;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

public class AzureClientFactory {

    private static final String SUBSCRIPTION_TEMPLATE = "Subscription : %s(%s)";
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account";
    public static final String NO_AVAILABLE_SUBSCRIPTION = "No available subscription found in current account";
    public static final String SUBSCRIPTION_NOT_SPECIFIED = "Subscription ID was not specified, using the first subscription in current account," +
            " please refer https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#subscription for more information";

    public static Azure getAzureClient(AuthType authType, AuthConfiguration auth, String subscriptionId) throws IOException, AzureLoginFailureException {
        final AzureEnvironment environment = AzureEnvironment.AZURE;
        final AzureTokenWrapper azureTokenCredentials = authType.getAzureToken(auth, environment);
        if (azureTokenCredentials != null) {
            Log.info(azureTokenCredentials.getCredentialDescription());
            final Authenticated authenticated = Azure.configure().authenticate(azureTokenCredentials);
            // For cloud shell, use subscription in profile as the default subscription.
            if (StringUtils.isEmpty(subscriptionId) && AzureAuthHelperLegacy.isInCloudShell()) {
                subscriptionId = AzureAuthHelperLegacy.getSubscriptionOfCloudShell();
            }
            subscriptionId = StringUtils.isEmpty(subscriptionId) ? azureTokenCredentials.defaultSubscriptionId() : subscriptionId;
            final Azure azureClient = StringUtils.isEmpty(subscriptionId) ? authenticated.withDefaultSubscription() :
                    authenticated.withSubscription(subscriptionId);
            checkSubscription(azureClient, subscriptionId);
            final Subscription subscription = azureClient.getCurrentSubscription();
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, subscription.displayName(), subscription.subscriptionId()));
            return azureClient;
        }
        return null;
    }

    private static void checkSubscription(Azure azure, String targetSubscription) throws AzureLoginFailureException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        subscriptions.loadAll();
        if (subscriptions.size() == 0) {
            throw new AzureLoginFailureException(NO_AVAILABLE_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(targetSubscription)) {
            Log.warn(SUBSCRIPTION_NOT_SPECIFIED);
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.subscriptionId(), targetSubscription))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new AzureLoginFailureException(String.format(SUBSCRIPTION_NOT_FOUND, targetSubscription));
        }
    }
}
