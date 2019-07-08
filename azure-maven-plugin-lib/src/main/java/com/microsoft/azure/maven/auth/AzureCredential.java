/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.maven.auth.oauth.AzureLoginHelper;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureCredential {
    private AzureEnvironment env;
    private transient AuthenticationResult authenticationResult;
    private String refreshToken;

    public AzureCredential(AzureEnvironment env, AuthenticationResult authenticationResult) {
        if (authenticationResult == null) {
            throw new NullPointerException("Parameter 'authenticationResult' cannot be null.");
        }
        this.env = env;
        this.authenticationResult = authenticationResult;
        this.refreshToken = authenticationResult.getRefreshToken();
    }

    public AzureTokenCredentials getCredential() {
        return new AzureTokenCredentials(null, null) {
            @Override
            public String getToken(String resource) throws IOException {
                if (authenticationResult.getExpiresOnDate().before(new Date())) {
                    refreshToken();
                }
                return authenticationResult.getAccessToken();
            }
        };
    }

    public void refreshToken() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            // TODO: handle proxy
            final AuthenticationContext authenticationContext = 
                    new AuthenticationContext(AzureLoginHelper.baseURL(env), true, executorService);
            final AuthenticationResult authenticationResult = 
                    authenticationContext.acquireTokenByRefreshToken(refreshToken, 
                            AzureLoginHelper.CLIENT_ID, env.managementEndpoint(), null).get();
            this.authenticationResult = authenticationResult;
            this.refreshToken = authenticationResult.getRefreshToken();
        } catch (Exception e) {
            // ignore            
            return;
        } finally {
            executorService.shutdown();
        }
    }
}
