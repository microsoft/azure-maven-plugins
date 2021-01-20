/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;

import com.google.common.base.MoreObjects;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.core.ChainedCredentialRetriever;
import com.microsoft.azure.tools.auth.core.ICredentialRetriever;
import com.microsoft.azure.tools.auth.core.azurecli.AzureCliCredentialRetriever;
import com.microsoft.azure.tools.auth.core.devicecode.DeviceCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.core.managedidentity.ManagedIdentityCredentialRetriever;
import com.microsoft.azure.tools.auth.core.oauth.OAuthCredentialRetriever;
import com.microsoft.azure.tools.auth.core.serviceprincipal.ServicePrincipalCredentialRetriever;
import com.microsoft.azure.tools.auth.core.visualstudio.VisualStudioCredentialRetriever;
import com.microsoft.azure.tools.auth.core.vscode.VisualStudioCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import com.microsoft.azure.tools.auth.model.AuthType;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;

import java.util.HashMap;
import java.util.Map;

public class AzureAuthManager {
    public static Single<AzureCredentialWrapper> getAzureCredentialWrapper(AuthConfiguration configuration) {
        AuthConfiguration auth = MoreObjects.firstNonNull(configuration, new AuthConfiguration());
        AuthHelper.setupAzureEnvironment(auth.getEnvironment());
        ChainedCredentialRetriever retrievers = new ChainedCredentialRetriever();
        AuthType authType = MoreObjects.firstNonNull(auth.getType(), AuthType.AUTO);
        Map<AuthType, ICredentialRetriever> retrieverMap = buildCredentialRetrievers(configuration);
        if (authType.equals(AuthType.AUTO)) {
            retrievers.addRetriever(retrieverMap.get(AuthType.SERVICE_PRINCIPAL));
            retrievers.addRetriever(retrieverMap.get(AuthType.MANAGED_IDENTITY));
            retrievers.addRetriever(retrieverMap.get(AuthType.AZURE_CLI));
            retrievers.addRetriever(retrieverMap.get(AuthType.VSCODE));
            retrievers.addRetriever(retrieverMap.get(AuthType.VISUAL_STUDIO));
            retrievers.addRetriever(retrieverMap.get(AuthType.OAUTH2));
            retrievers.addRetriever(retrieverMap.get(AuthType.DEVICE_CODE));
        } else {
            // for specific auth type:
            if (!retrieverMap.containsKey(authType)) {
                return Single.error(new UnsupportedOperationException(String.format("authType '%s' not supported.", authType)));
            }
            retrievers.addRetriever(retrieverMap.get(authType));
        }
        return retrievers.retrieve().onErrorResumeNext(e ->
                Single.error(new LoginFailureException(String.format("Cannot get credentials from authType '%s' due to error: %s", authType, e.getMessage())))
        );
    }

    private static Map<AuthType, ICredentialRetriever> buildCredentialRetrievers(AuthConfiguration auth) {
        AzureEnvironment env = auth.getEnvironment();
        Map<AuthType, ICredentialRetriever> map = new HashMap<>();
        map.put(AuthType.SERVICE_PRINCIPAL, new ServicePrincipalCredentialRetriever(auth, env));
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityCredentialRetriever(env));
        map.put(AuthType.AZURE_CLI, new AzureCliCredentialRetriever(env));
        map.put(AuthType.VSCODE, new VisualStudioCodeCredentialRetriever(env));
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioCredentialRetriever(env));
        map.put(AuthType.OAUTH2, new OAuthCredentialRetriever(env));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeCredentialRetriever(env));
        return map;
    }
}
