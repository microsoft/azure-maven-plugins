/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.AzureTokenCredentialsDecorator;
import com.microsoft.azure.auth.configuration.AuthConfiguration;
import com.microsoft.azure.auth.configuration.AuthType;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AzureClientFactory {
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
            System.out.println(String.format("AuthMethod: %s", azureTokenCredentials.getAuthType()));
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
