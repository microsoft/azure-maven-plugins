/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.core.AzureCliCredentialRetriever;
import com.microsoft.azure.tools.auth.core.ChainedCredentialRetriever;
import com.microsoft.azure.tools.auth.core.DeviceCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.core.ManagedIdentityCredentialRetriever;
import com.microsoft.azure.tools.auth.core.OAuthCredentialRetriever;
import com.microsoft.azure.tools.auth.core.ServicePrincipalCredentialRetriever;
import com.microsoft.azure.tools.auth.core.VisualStudioCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.core.VisualStudioCredentialRetriever;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import com.microsoft.azure.tools.auth.model.AuthType;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

public class AzureAuthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAuthManager.class);
    private static final String FALLBACK_TO_AZURE_ENVIRONMENT = "Unsupported Azure environment %s, using Azure by default.";
    private static final String FALLBACK_TO_AUTO_AUTH_TYPE = "Will use 'auto' by default.";

    public static Single<AzureCredentialWrapper> getAzureCredentialWrapper(final AuthConfiguration configuration) {
        AzureEnvironment env = getAzureEnvironmentFromConfiguration(configuration);
        ChainedCredentialRetriever retrievers = new ChainedCredentialRetriever();

        AuthType authType = getAuthTypeFromConfiguration(configuration);
        if (authType.equals(AuthType.AUTO)) {
            if (!StringUtils.isAllBlank(configuration.getCertificate(), configuration.getKey(), configuration.getCertificatePassword())) {
                retrievers.addRetriever(new ServicePrincipalCredentialRetriever(configuration)::retrieve);
            } else {
                retrievers.addRetriever(new ManagedIdentityCredentialRetriever(env)::retrieve);
                retrievers.addRetriever(new AzureCliCredentialRetriever()::retrieve);
                retrievers.addRetriever(new VisualStudioCodeCredentialRetriever()::retrieve);
                retrievers.addRetriever(new VisualStudioCredentialRetriever(env)::retrieve);
                retrievers.addRetriever(new OAuthCredentialRetriever(env)::retrieve);
                retrievers.addRetriever(new DeviceCodeCredentialRetriever(env)::retrieve);
            }

        } else {
            // for specific auth type:
            switch (authType) {
                case SERVICE_PRINCIPAL: // will get configuration from auth configuration
                    retrievers.addRetriever(new ServicePrincipalCredentialRetriever(configuration));
                    break;
                case MANAGED_IDENTITY:
                    retrievers.addRetriever(new ManagedIdentityCredentialRetriever(env)::retrieve);
                    break;
                case AZURE_CLI:
                    retrievers.addRetriever(new AzureCliCredentialRetriever()::retrieve);
                    break;
                case VSCODE:
                    retrievers.addRetriever(new VisualStudioCodeCredentialRetriever()::retrieve);
                    break;
                case VISUAL_STUDIO:
                    retrievers.addRetriever(new VisualStudioCredentialRetriever(env)::retrieve);
                    break;
                case OAUTH2:
                    retrievers.addRetriever(new OAuthCredentialRetriever(env)::retrieve);
                    break;
                case DEVICE_CODE:
                    retrievers.addRetriever(new DeviceCodeCredentialRetriever(env)::retrieve);
                    break;
                default:
                    return Single.error(new UnsupportedOperationException(String.format("authType '%s' not supported.", authType)));
            }
        }
        return retrievers.retrieve().onErrorResumeNext(e ->
                Single.error(new LoginFailureException(String.format("Cannot get credentials from authType '%s' due to error: %s", authType, e.getMessage())))
        );
    }

    private static AzureEnvironment getAzureEnvironmentFromConfiguration(AuthConfiguration configuration) {
        AzureEnvironment env = AuthHelper.parseAzureEnvironment(configuration.getEnvironment());
        if (env == null) {
            env = AzureEnvironment.AZURE;
            if (!AuthHelper.validateEnvironment(configuration.getEnvironment())) {
                LOGGER.warn(String.format(FALLBACK_TO_AZURE_ENVIRONMENT, configuration.getEnvironment()));
            }
        }
        return env;
    }

    private static AuthType getAuthTypeFromConfiguration(AuthConfiguration configuration) {
        AuthType authType;
        try {
            authType = AuthType.parseAuthType(configuration.getType());
        } catch (InvalidConfigurationException e) {
            LOGGER.warn(e.getMessage());
            LOGGER.warn(FALLBACK_TO_AUTO_AUTH_TYPE);
            authType = AuthType.AUTO;
        }
        return authType;
    }
}
