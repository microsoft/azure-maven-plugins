/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.core.profile.VisualStudioCodeProfileRetriever;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.auth.model.VisualStudioCodeAccountProfile;

import java.util.Objects;

public class VisualStudioCodeCredentialRetriever extends AbstractCredentialRetriever {
    public VisualStudioCodeCredentialRetriever(AzureEnvironment env) {
        super(env);
    }

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        final VisualStudioCodeAccountProfile vscodeProfile = VisualStudioCodeProfileRetriever.getProfile();
        if (Objects.isNull(vscodeProfile)) {
            throw new LoginFailureException("Cannot get azure credentials from VSCode, please verify that you have signed-in in VSCode Azure Account plugin.");
        }
        AzureEnvironment envFromVSCode = AuthHelper.parseAzureEnvironment(vscodeProfile.getEnvironment());
        if (envFromVSCode != null && env != null && envFromVSCode != env) {
            final String envNameFromVSCode = AuthHelper.getAzureCloudDisplayName(envFromVSCode);
            throw new LoginFailureException(String.format("The azure cloud from vscode '%s' doesn't match with your auth configuration: %s, " +
                            "you can change it by press F1 and find \">azure: sign in to Azure Cloud\" command to change azure cloud in vscode.",
                    envNameFromVSCode,
                    AuthHelper.getAzureCloudDisplayName(env)));
        }
        this.env = envFromVSCode;
        AuthHelper.setupAzureEnvironment(env);
        return vsCodeLogin(vscodeProfile);
    }

    private AzureCredentialWrapper vsCodeLogin(VisualStudioCodeAccountProfile profile) throws LoginFailureException {
        final VisualStudioCodeCredential visualStudioCodeCredential = new VisualStudioCodeCredentialBuilder().build();
        validateTokenCredential(visualStudioCodeCredential);
        return new AzureCredentialWrapper(AuthMethod.VSCODE, visualStudioCodeCredential, getAzureEnvironment())
                .withFilteredSubscriptionIds(profile.getFilteredSubscriptions());
    }
}
