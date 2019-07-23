/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
        getLog().info("Begin to login");
        AzureCredential cred = null;
        try {
            cred = devicelogin ? null : AzureAuthHelper.oAuthLogin(AzureEnvironment.AZURE);

            if (cred == null) {
                // fallback to device login if oauth login fails
                cred = AzureAuthHelper.deviceLogin(env);
            }
            // device login will either success or either throw AzureLoginFailureException
            AzureAuthHelper.writeAzureCredentials(cred, AzureAuthHelper.getAzureSecretFile());

        } catch (Exception e) {
            throw new MojoFailureException(String.format("Fail to login due to error: %s.", e.getMessage()));
        }
    }
}
