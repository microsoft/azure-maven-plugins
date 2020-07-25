/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Enum for customer specified auth type
 */
public enum AuthType {
    AZURE_CLI {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getAzureCLICredential(environment);
            } catch (IOException e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    AZURE_AUTH_MAVEN_PLUGIN {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getAzureMavenPluginCredential(environment);
            } catch (IOException | InterruptedException | ExecutionException e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    // Used for auto only, as we need to check existing azure maven plugin credential first and then azure cli
    AZURE_SECRET_FILE {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getAzureSecretFileCredential();
            } catch (IOException e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    SERVICE_PRINCIPAL {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getServicePrincipalCredential(configuration);
            } catch (Exception e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    AUTO {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) {
            return AzureAuthHelper.getAzureCredentialByOrder(configuration, environment);
        }
    };

    public static AuthType[] getValidAuthTypes() {
        return new AuthType[]{SERVICE_PRINCIPAL, AZURE_AUTH_MAVEN_PLUGIN, AZURE_CLI, AUTO};
    }

    public abstract AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException;
}
