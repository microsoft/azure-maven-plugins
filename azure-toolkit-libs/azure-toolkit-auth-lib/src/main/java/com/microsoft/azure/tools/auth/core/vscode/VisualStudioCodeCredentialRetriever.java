/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core.vscode;

import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import com.google.common.base.MoreObjects;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.core.AbstractCredentialRetriever;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;

import java.util.Objects;

public class VisualStudioCodeCredentialRetriever extends AbstractCredentialRetriever {
    public VisualStudioCodeCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        try {
            final VisualStudioCodeAccountProfile vscodeProfile = VisualStudioCodeProfileRetriever.getProfile();
            AzureEnvironment envFromVscode = AuthHelper.stringToAzureEnvironment(vscodeProfile.getEnvironment());
            checkAzureEnvironmentConflict(env, envFromVscode);
            AuthHelper.setupAzureEnvironment(envFromVscode);
            final VisualStudioCodeCredential visualStudioCodeCredential = new VisualStudioCodeCredentialBuilder().build();
            validateTokenCredential(visualStudioCodeCredential);
            return new AzureCredentialWrapper(AuthMethod.VSCODE, visualStudioCodeCredential, MoreObjects.firstNonNull(envFromVscode, AzureEnvironment.AZURE))
                    .withFilteredSubscriptionIds(vscodeProfile.getFilteredSubscriptions());
        } catch (InvalidConfigurationException e) {
            throw new LoginFailureException("Cannot get azure profile from VSCode, please make sure that you have signed-in in VSCode Azure Account plugin," +
                    " detailed messages is: %s");
        }
    }

    private static void checkAzureEnvironmentConflict(AzureEnvironment env, AzureEnvironment envVSCode) throws LoginFailureException {
        if (env != null && envVSCode != null && !Objects.equals(env, envVSCode)) {
            throw new LoginFailureException(String.format("The azure cloud from vscode '%s' doesn't match with your auth configuration: %s, " +
                            "you can change it by press F1 and find \">azure: sign in to Azure Cloud\" command to change azure cloud in vscode.",
                    AuthHelper.azureEnvironmentToString(envVSCode), AuthHelper.azureEnvironmentToString(env)));
        }
    }
}
