/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import com.microsoft.azure.tools.exception.DesktopNotSupportedException;
import com.microsoft.azure.common.utils.TextUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.microsoft.azure.plugin.login.Constant.TELEMETRY_KEY_AUTH_METHOD;
import static com.microsoft.azure.plugin.login.Constant.TELEMETRY_KEY_IS_BROWSER_AVAILABLE;
import static com.microsoft.azure.plugin.login.Constant.TELEMETRY_KEY_IS_LOGGED_IN;
import static com.microsoft.azure.plugin.login.Constant.TELEMETRY_VALUE_AUTH_METHOD_DEVICE;
import static com.microsoft.azure.plugin.login.Constant.TELEMETRY_VALUE_AUTH_METHOD_OAUTH;

/**
 * Goal to login to azure.
 */
@Mojo(name = "login", aggregator = true)
public class LoginMojo extends AbstractAzureMojo {

    @Parameter(property = "devicelogin")
    protected boolean devicelogin;

    @Parameter(property = "environment")
    protected String environment;

    @Override
    public void doExecute() throws MojoFailureException {
        final AzureEnvironment env = AzureAuthHelper.getAzureEnvironment(environment);
        AzureCredential newAzureCredential;

        try {
            String previousSubscriptionId = null;
            try {
                previousSubscriptionId = AzureAuthHelper.existsAzureSecretFile() ?
                        AzureAuthHelper.readAzureCredentials(AzureAuthHelper.getAzureSecretFile()).getDefaultSubscription() :
                        null;
            } catch (IOException e) {
                // ignore;
            }

            String loginMethod = devicelogin ? TELEMETRY_VALUE_AUTH_METHOD_DEVICE : TELEMETRY_VALUE_AUTH_METHOD_OAUTH;
            try {
                newAzureCredential = devicelogin ? AzureAuthHelper.deviceLogin(env) : AzureAuthHelper.oAuthLogin(env);
            } catch (DesktopNotSupportedException e) {
                // fallback to device login if oauth login fails
                newAzureCredential = AzureAuthHelper.deviceLogin(env);
                loginMethod = TELEMETRY_VALUE_AUTH_METHOD_DEVICE;
            }
            trackLoginMethod(loginMethod);

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
            final File secretFile = AzureAuthHelper.getAzureSecretFile();
            AzureAuthHelper.writeAzureCredentials(newAzureCredential, secretFile);
            getLog().info(String.format("The azure credential has been saved to file: %s.", TextUtils.blue(secretFile.getAbsolutePath())));
        } catch (AzureLoginFailureException | ExecutionException | InterruptedException | IOException e) {
            throw new MojoFailureException(String.format("Fail to login due to error: %s.", e.getMessage()));
        }
    }

    protected void trackLoginMethod(final String loginMethod) {
        final boolean isLoggedIn = AzureAuthHelper.existsAzureSecretFile();
        final boolean isBrowserAvailable = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        getTelemetries().put(TELEMETRY_KEY_AUTH_METHOD, loginMethod);
        getTelemetries().put(TELEMETRY_KEY_IS_LOGGED_IN, String.valueOf(isLoggedIn));
        getTelemetries().put(TELEMETRY_KEY_IS_BROWSER_AVAILABLE, String.valueOf(isBrowserAvailable));
    }
}
