/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.AzureLoginFailureException;
import com.microsoft.azure.auth.DesktopNotSupportedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Goal to login to azure.
 */
@Mojo(name = "login", inheritByDefault = true, aggregator = true)
public class LoginMojo extends AbstractMojo {

    @Parameter(property = "devicelogin")
    public boolean devicelogin;

    @Parameter(property = "environment")
    public String environment;

    @Override
    public void execute() throws MojoFailureException {
        final AzureEnvironment env = AzureAuthHelper.getAzureEnvironment(environment);
        AzureCredential newAzureCredential = null;
        try {

            String previousSubscriptionId = null;
            try {
                previousSubscriptionId = AzureAuthHelper.existsAzureSecretFile() ?
                        AzureAuthHelper.readAzureCredentials(AzureAuthHelper.getAzureSecretFile()).getDefaultSubscription() :
                        null;
            } catch (IOException e) {
                // ignore;
            }

            try {
                newAzureCredential = devicelogin ? AzureAuthHelper.deviceLogin(env) : AzureAuthHelper.oAuthLogin(env);
            } catch (DesktopNotSupportedException e) {
                // fallback to device login if oauth login fails
                newAzureCredential = AzureAuthHelper.deviceLogin(env);
            }

            if (StringUtils.isNotBlank(previousSubscriptionId)) {
                // save the older subscription id if it is valid
                final Authenticated azure = Azure.configure().authenticate(AzureAuthHelper.getMavenAzureLoginCredentials(newAzureCredential, env));
                for (final Subscription subscription : azure.subscriptions().list()) {
                    if (StringUtils.equalsIgnoreCase(previousSubscriptionId, subscription.subscriptionId())) {
                        newAzureCredential.setDefaultSubscription(previousSubscriptionId);
                        break;
                    }
                }
            }

            // device login will either success or either throw AzureLoginFailureException
            AzureAuthHelper.writeAzureCredentials(newAzureCredential, AzureAuthHelper.getAzureSecretFile());

        } catch (AzureLoginFailureException | ExecutionException | InterruptedException | IOException e) {
            throw new MojoFailureException(String.format("Fail to login due to error: %s.", e.getMessage()));
        }
    }
}
