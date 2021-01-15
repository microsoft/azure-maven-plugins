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
import com.microsoft.azure.tools.auth.core.ICredentialRetriever;
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

import java.util.HashMap;
import java.util.Map;

public class AzureAuthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAuthManager.class);
    private static final String FALLBACK_TO_AZURE_ENVIRONMENT = "Unsupported Azure environment %s, using Azure by default.";
    private static final String FALLBACK_TO_AUTO_AUTH_TYPE = "Will use 'auto' by default.";

    public static Single<AzureCredentialWrapper> getAzureCredentialWrapper(AuthConfiguration configuration) {
        AuthConfiguration auth = configuration == null ? new AuthConfiguration() : configuration;
        AzureEnvironment env = getAzureEnvironmentFromString(auth.getEnvironment());
        ChainedCredentialRetriever retrievers = new ChainedCredentialRetriever();
        AuthType authType = getAuthTypeFromString(auth.getType());
        Map<AuthType, ICredentialRetriever> retrieverMap = buildCredentialRetrievers(env);
        if (authType.equals(AuthType.AUTO)) {
            if (!StringUtils.isAllBlank(auth.getCertificate(), auth.getKey(), auth.getCertificatePassword())) {
                retrievers.addRetriever(new ServicePrincipalCredentialRetriever(auth, env));
            } else {
                retrievers.addRetriever(retrieverMap.get(AuthType.MANAGED_IDENTITY));
                retrievers.addRetriever(retrieverMap.get(AuthType.AZURE_CLI));
                retrievers.addRetriever(retrieverMap.get(AuthType.VSCODE));
                retrievers.addRetriever(retrieverMap.get(AuthType.VISUAL_STUDIO));
                retrievers.addRetriever(retrieverMap.get(AuthType.OAUTH2));
                retrievers.addRetriever(retrieverMap.get(AuthType.DEVICE_CODE));

                retrievers.addRetriever(new AzureCliCredentialRetriever(env));
                retrievers.addRetriever(new VisualStudioCodeCredentialRetriever(env));
                retrievers.addRetriever(new VisualStudioCredentialRetriever(env));
                retrievers.addRetriever(new OAuthCredentialRetriever(env));
                retrievers.addRetriever(new DeviceCodeCredentialRetriever(env));
            }

        } else {
            // for specific auth type:
            if (AuthType.SERVICE_PRINCIPAL == authType) {
                retrievers.addRetriever(new ServicePrincipalCredentialRetriever(auth, env));
            } else {
                if (!retrieverMap.containsKey(authType)) {
                    return Single.error(new UnsupportedOperationException(String.format("authType '%s' not supported.", authType)));
                }
                retrievers.addRetriever(retrieverMap.get(authType));
            }
        }
        return retrievers.retrieve().onErrorResumeNext(e ->
                Single.error(new LoginFailureException(String.format("Cannot get credentials from authType '%s' due to error: %s", authType, e.getMessage())))
        );
    }

    private static Map<AuthType, ICredentialRetriever> buildCredentialRetrievers(AzureEnvironment env) {
        Map<AuthType, ICredentialRetriever> map = new HashMap<>();
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityCredentialRetriever(env));
        map.put(AuthType.AZURE_CLI, new AzureCliCredentialRetriever(env));
        map.put(AuthType.VSCODE, new VisualStudioCodeCredentialRetriever(env));
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioCredentialRetriever(env));
        map.put(AuthType.OAUTH2, new OAuthCredentialRetriever(env));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeCredentialRetriever(env));
        return map;
    }

    private static AzureEnvironment getAzureEnvironmentFromString(String envString) {
        if (!AuthHelper.validateEnvironment(envString)) {
            LOGGER.warn(String.format(FALLBACK_TO_AZURE_ENVIRONMENT, envString));
            return AzureEnvironment.AZURE;
        }
        return AuthHelper.parseAzureEnvironment(envString);
    }

    private static AuthType getAuthTypeFromString(String authTypeString) {
        AuthType authType;
        try {
            authType = AuthType.parseAuthType(authTypeString);
        } catch (InvalidConfigurationException e) {
            LOGGER.warn(e.getMessage());
            LOGGER.warn(FALLBACK_TO_AUTO_AUTH_TYPE);
            authType = AuthType.AUTO;
        }
        return authType;
    }
}
