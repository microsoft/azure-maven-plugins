/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.google.common.base.Preconditions;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;

import com.microsoft.azure.tools.auth.exception.AzureLoginException;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

public class AzureClientFactory {
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account.";
    public static final String NO_AVAILABLE_SUBSCRIPTION = "No available subscription found in current account.";
    public static final String SUBSCRIPTION_NOT_SPECIFIED = "Subscription ID was not specified, using the first subscription in current account," +
            " please refer https://github.com/microsoft/azure-maven-plugins/wiki/Authentication#subscription for more information.";

    public static Azure getAzureClient(AzureCredentialWrapper azureTokenCredentials, String subsId,
                                       String userAgent) throws IOException, AzureLoginException {
        Preconditions.checkNotNull(azureTokenCredentials, "The parameter 'azureTokenCredentials' cannot be null.");
        String subscriptionId = subsId;
        Log.info(azureTokenCredentials.getCredentialDescription());
        final Authenticated authenticated = Azure.configure().withUserAgent(userAgent).authenticate(azureTokenCredentials.getAzureTokenCredentials());
        // For cloud shell, use subscription in profile as the default subscription.
        if (StringUtils.isEmpty(subscriptionId) && AzureAuthHelperLegacy.isInCloudShell()) {
            subscriptionId = AzureAuthHelperLegacy.getSubscriptionOfCloudShell();
        }
        subscriptionId = StringUtils.isEmpty(subscriptionId) ? azureTokenCredentials.getDefaultSubscriptionId() : subscriptionId;
        final Azure azureClient = StringUtils.isEmpty(subscriptionId) ? authenticated.withDefaultSubscription() :
                authenticated.withSubscription(subscriptionId);
        checkSubscription(azureClient, subscriptionId);
        return azureClient;
    }

    private static void checkSubscription(Azure azure, String targetSubscription) throws AzureLoginException {
        final PagedList<Subscription> subscriptions = azure.subscriptions().list();
        subscriptions.loadAll();
        if (subscriptions.size() == 0) {
            throw new AzureLoginException(NO_AVAILABLE_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(targetSubscription)) {
            Log.warn(SUBSCRIPTION_NOT_SPECIFIED);
            return;
        }
        final Optional<Subscription> optionalSubscription = subscriptions.stream()
                .filter(subscription -> StringUtils.equals(subscription.subscriptionId(), targetSubscription))
                .findAny();
        if (!optionalSubscription.isPresent()) {
            throw new AzureLoginException(String.format(SUBSCRIPTION_NOT_FOUND, targetSubscription));
        }
    }
}
