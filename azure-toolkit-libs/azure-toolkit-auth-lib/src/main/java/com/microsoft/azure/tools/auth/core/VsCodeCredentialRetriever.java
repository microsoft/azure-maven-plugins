/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.identity.VisualStudioCodeCredential;
import com.azure.identity.VisualStudioCodeCredentialBuilder;
import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.core.profile.VsCodeProfileRetriever;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import com.microsoft.azure.tools.auth.model.VsCodeAccountProfile;
import org.apache.commons.lang3.ArrayUtils;

public class VsCodeCredentialRetriever extends AbstractCredentialRetriever {

    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        VsCodeAccountProfile[] vscodeProfiles = VsCodeProfileRetriever.getProfiles();
        if (ArrayUtils.isEmpty(vscodeProfiles)) {
            throw new LoginFailureException("Cannot get azure credentials from VSCode, please verify that you have signed-in in VSCode Azure Account plugin.");
        }

        return vsCodeLogin(vscodeProfiles);
    }

    private static AzureCredentialWrapper vsCodeLogin(VsCodeAccountProfile[] profiles) {
        VisualStudioCodeCredential visualStudioCodeCredential = new VisualStudioCodeCredentialBuilder().build();
        String env = profiles[0].getEnvironment();
        return new AzureCredentialWrapper(AuthMethod.VSCODE, visualStudioCodeCredential, AuthHelper.parseAzureEnvironment(env))
                .withTenantId(profiles[0].getTenantId())
                .withDefaultSubscriptionId(profiles[0].getSubscriptionId());
    }
}
