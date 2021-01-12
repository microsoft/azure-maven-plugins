/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.core;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.CredentialUnavailableException;
import com.azure.identity.SharedTokenCacheCredentialBuilder;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthMethod;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;

public class VisualStudioCredentialRetriever extends AbstractCredentialRetriever {
    private static final String AZURE_CLI_CLIENT_ID = "04b07795-8ddb-461a-bbee-02f9e1bf7b46";

    public VisualStudioCredentialRetriever(AzureEnvironment env) {
        this.env = env;
    }

    @Override
    public AzureCredentialWrapper retrieveInternal() throws LoginFailureException {
        final TokenCredential credential = new SharedTokenCacheCredentialBuilder().clientId(AZURE_CLI_CLIENT_ID).build();
        try {
            credential.getToken(new TokenRequestContext().addScopes(env.managementEndpoint() + ".default")).block().getToken();
            return new AzureCredentialWrapper(AuthMethod.VISUAL_STUDIO, credential, env);
        } catch (CredentialUnavailableException ex) {
            throw new LoginFailureException(ex.getMessage(), ex);
        }
    }
}
