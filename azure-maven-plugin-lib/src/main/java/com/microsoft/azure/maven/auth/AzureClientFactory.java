/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;

public class AzureClientFactory {
    public static Azure getAzureClient(AuthConfiguration auth, String subscriptionId) throws InvalidConfigurationException, IOException,
        AzureLoginFailureException, InterruptedException, ExecutionException {
        AzureTokenCredentials azureTokenCredentials = AzureAuthHelper.getAzureTokenCredentials(auth);
        if (azureTokenCredentials == null) {
            final AzureEnvironment environment = AzureEnvironment.AZURE;
            AzureCredential azureCredential;
            try {
                azureCredential = AzureAuthHelper.oAuthLogin(environment);
            } catch (DesktopNotSupportedException e) {
                azureCredential = AzureAuthHelper.deviceLogin(environment);
            }
            AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
            azureTokenCredentials = AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment);
        }

        if (azureTokenCredentials != null) {
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
