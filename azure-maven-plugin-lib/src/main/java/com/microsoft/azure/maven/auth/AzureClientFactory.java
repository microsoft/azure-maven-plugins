/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.configuration.AuthType;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AzureClientFactory {

    private static final String SUBSCRIPTION_TEMPLATE = "Subscription : %s(%s)";
    public static final String SUBSCRIPTION_NOT_FOUND = "Subscription %s was not found in current account";
    public static final String NO_AVAILABLE_SUBSCRIPTION = "No available subscription found in current account";

    public static Azure getAzureClient(AuthConfiguration auth, String subscriptionId) throws InvalidConfigurationException, IOException,
            AzureLoginFailureException, InterruptedException, ExecutionException {
        AzureTokenWrapper azureTokenCredentials = AzureAuthHelper.getAzureTokenCredentials(auth);
        if (azureTokenCredentials == null) {
            final AzureEnvironment environment = AzureEnvironment.AZURE;
            AzureCredential azureCredential;
            AuthType authType;
            try {
                authType = AuthType.OAUTH;
                azureCredential = AzureAuthHelper.oAuthLogin(environment);
            } catch (DesktopNotSupportedException e) {
                authType = AuthType.DEVICE_LOGIN;
                azureCredential = AzureAuthHelper.deviceLogin(environment);
            }
            AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
            azureTokenCredentials = new AzureTokenWrapper(authType, AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment));
        }

        if (azureTokenCredentials != null) {
            Log.info(azureTokenCredentials.getCredentialDescription());
            final Authenticated authenticated = Azure.configure().authenticate(azureTokenCredentials);
            // For cloud shell, use subscription in profile as the default subscription.
            if (StringUtils.isEmpty(subscriptionId) && AzureAuthHelperLegacy.isInCloudShell()) {
                subscriptionId = AzureAuthHelperLegacy.getSubscriptionOfCloudShell();
            }
            return StringUtils.isEmpty(subscriptionId) ?
                    authenticated.withDefaultSubscription() :
                    authenticated.withSubscription(subscriptionId);
        }

        return null;
    }
}
