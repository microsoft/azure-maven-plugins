/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.AzureTokenWrapper;
import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;

public enum AuthType {
    AZURE_CLI {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getAzureCLICredential();
            } catch (IOException e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    DEVICE_LOGIN {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment)
                throws AzureLoginFailureException {
            try {
                final AzureCredential azureCredential = AzureAuthHelper.deviceLogin(environment);
                AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
                return new AzureTokenWrapper(DEVICE_LOGIN, AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment));
            } catch (Exception e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    MSI {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) {
            return AzureAuthHelper.getMSICredential();
        }
    },
    OAUTH {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment)
                throws AzureLoginFailureException {
            try {
                final AzureCredential azureCredential = AzureAuthHelper.oAuthLogin(environment);
                AzureAuthHelper.writeAzureCredentials(azureCredential, AzureAuthHelper.getAzureSecretFile());
                return new AzureTokenWrapper(OAUTH, AzureAuthHelper.getMavenAzureLoginCredentials(azureCredential, environment));
            } catch (Exception e) {
                throw new AzureLoginFailureException(e.getMessage());
            }
        }
    },
    SECRET_FILE {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException {
            try {
                return AzureAuthHelper.getSecretFileCredential();
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
    UNKNOWN {
        @Override
        public AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) {
            return null;
        }
    };

    public static AuthType fromString(String input) {
        return StringUtils.isEmpty(input) ? UNKNOWN :
                Arrays.stream(AuthType.values())
                        .filter(authType -> StringUtils.equalsIgnoreCase(input, authType.name()))
                        .findFirst().orElse(UNKNOWN);
    }

    public abstract AzureTokenWrapper getAzureToken(AuthConfiguration configuration, AzureEnvironment environment) throws AzureLoginFailureException;
}
