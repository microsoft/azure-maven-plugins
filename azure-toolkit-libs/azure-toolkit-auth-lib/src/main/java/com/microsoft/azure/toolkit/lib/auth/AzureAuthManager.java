/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.auth.core.ICredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.core.devicecode.DeviceCodeCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.core.managedidentity.ManagedIdentityCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.core.serviceprincipal.ServicePrincipalCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.core.visualstudio.VisualStudioCredentialRetriever;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCredentialWrapper;
import rx.Single;

import java.util.LinkedHashMap;
import java.util.Map;

public class AzureAuthManager {
    protected Single<AzureCredentialWrapper> login(AuthConfiguration configuration) {
        buildCredentialRetrievers(configuration);
        return null;
    }

    private static Map<AuthType, ICredentialRetriever> buildCredentialRetrievers(AuthConfiguration auth) {
        AzureEnvironment env = auth.getEnvironment();
        Map<AuthType, ICredentialRetriever> map = new LinkedHashMap<>();
        map.put(AuthType.SERVICE_PRINCIPAL, new ServicePrincipalCredentialRetriever(auth));
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityCredentialRetriever(auth));
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioCredentialRetriever(env));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeCredentialRetriever(env));
        return map;
    }

}
